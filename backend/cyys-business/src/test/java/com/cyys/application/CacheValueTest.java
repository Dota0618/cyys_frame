package com.cyys.application;

import com.cyys.admin.user.dto.UserLabel;
import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.common.cache.*;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheValueTest {
    public record Whole(String id, @CacheValue(cache="user", key="id") UserLabel user) {}
    public record Name(String id, @CacheValue(cache="user", key="id", field="name") String name) {}
    public record MissingField(String id, @CacheValue(cache="user", key="id", field="unknown") String name) {}
    public record BadType(String id, @CacheValue(cache="user", key="id", field="name") Integer name) {}
    public record BadWhole(String id, @CacheValue(cache="user", key="id") DictionaryOption user) {}
    public record BadList(@CacheValue(cache="options") List<UserLabel> options) {}
    public record UnitName(String id, String scope,
            @CacheValue(cache="user", key="id", scopeKey="scope", field="name") String name) {}
    public record ListLookup(String code,
            @CacheValue(cache="options", key="code", field="label") String label,
            @CacheValue(cache="options", key="code") DictionaryOption option,
            @CacheValue(cache="options") List<DictionaryOption> all) {}

    @AfterEach void clearContext() { ScopeContext.clear(); }

    @SuppressWarnings("unchecked")
    private CacheValueSource<UserLabel> userSource() {
        CacheValueSource<UserLabel> source = mock(CacheValueSource.class);
        when(source.name()).thenReturn("user"); when(source.scope()).thenReturn(CacheScope.SYSTEM);
        when(source.shape()).thenReturn(CacheShape.RECORD); when(source.recordType()).thenReturn(UserLabel.class);
        return source;
    }
    @SuppressWarnings("unchecked")
    private CacheValueSource<DictionaryOption> listSource() {
        CacheValueSource<DictionaryOption> source = mock(CacheValueSource.class);
        when(source.name()).thenReturn("options"); when(source.scope()).thenReturn(CacheScope.SYSTEM);
        when(source.shape()).thenReturn(CacheShape.LIST); when(source.listKey()).thenReturn("value");
        when(source.recordType()).thenReturn(DictionaryOption.class);
        return source;
    }

    @Test void fieldsAndWholeRecordsShareOneBatchAndNullAndEmptyValuesAreValid() {
        var source = userSource();
        when(source.records(null,"",List.of("u","empty","null","missing")))
                .thenReturn(Map.of("u",new UserLabel("u","姓名",true),"empty",new UserLabel("empty","",false),"null",new UserLabel("null",null,false)));
        var processor = new CachedResponseValues(List.of(source));
        List<?> result = (List<?>)processor.enrich(List.of(new Name("u",null),new Whole("u",null),new Name("empty",null),
                new Name("null",null),new Name("missing","原值"),new MissingField("u","原值")));
        assertThat(result.get(0)).isEqualTo(new Name("u","姓名"));
        assertThat(result.get(1)).isEqualTo(new Whole("u",new UserLabel("u","姓名",true)));
        assertThat(result.get(2)).isEqualTo(new Name("empty",""));
        assertThat(result.get(3)).isEqualTo(new Name("null",null));
        assertThat(result.get(4)).isEqualTo(new Name("missing",null));
        assertThat(result.get(5)).isEqualTo(new MissingField("u",null));
        verify(source, times(1)).records(null,"",List.of("u","empty","null","missing"));
    }

    @Test void declaredTypeMismatchFailsEvenWhenKeyOrValueIsNull() {
        var source=userSource(); var processor=new CachedResponseValues(List.of(source));
        assertThatThrownBy(() -> processor.enrich(new BadType(null,null))).hasMessageContaining("类型不匹配");
        assertThatThrownBy(() -> processor.enrich(new BadWhole("missing",null))).hasMessageContaining("类型不匹配");
        verify(source, never()).records(any(),anyString(),anyList());
        var options=listSource();
        assertThatThrownBy(() -> new CachedResponseValues(List.of(options)).enrich(new BadList(null))).hasMessageContaining("类型不匹配");
    }

    @Test void listCanSupplyAFieldAnItemAndEntireListUsingOneLoad() {
        var source=listSource(); var option=new DictionaryOption("1","启用",false);
        when(source.list(null,"")).thenReturn(List.of(option));
        var processor=new CachedResponseValues(List.of(source));
        List<?> result=(List<?>)processor.enrich(List.of(new ListLookup("1",null,null,null),new ListLookup("missing",null,null,null)));
        assertThat(result.get(0)).isEqualTo(new ListLookup("1","启用",option,List.of(option)));
        assertThat(result.get(1)).isEqualTo(new ListLookup("missing",null,null,List.of(option)));
        verify(source,times(1)).list(null,"");
    }

    @Test void failedResponsesDoNotTriggerCacheSources() {
        var source=userSource(); var processor=new CachedResponseValues(List.of(source));
        var failure=R.fail(403,"无权访问"); failure.setData(new Name("u",null));
        assertThat(processor.enrich(failure)).isSameAs(failure);
        verify(source,never()).records(any(),anyString(),anyList());
    }

    @Test void nestedResponsesPreservePageMetadataNullsAndMapOrder() {
        var source = userSource();
        when(source.records(null, "", List.of("u"))).thenReturn(Map.of("u", new UserLabel("u", "姓名", true)));
        var page = new Page<Name>(2, 20, 51);
        page.setRecords(List.of(new Name("u", null)));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("page", page);
        data.put("items", Arrays.asList(new Name("u", null), null, "原值"));
        data.put("empty", null);
        var response = R.ok(data);

        var result = (R<?>) new CachedResponseValues(List.of(source)).enrich(response);
        assertThat(result.getTimestamp()).isEqualTo(response.getTimestamp());
        assertThat(result.getCode()).isEqualTo(response.getCode());
        assertThat(result.getMsg()).isEqualTo(response.getMsg());
        var enriched = (Map<?, ?>) result.getData();
        assertThat(new ArrayList<>(enriched.keySet())).isEqualTo(List.of("page", "items", "empty"));
        assertThat(enriched.get("page")).isSameAs(page);
        assertThat(page.getCurrent()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getTotal()).isEqualTo(51);
        assertThat(page.getRecords()).containsExactly(new Name("u", "姓名"));
        assertThat(enriched.get("items")).isEqualTo(Arrays.asList(new Name("u", "姓名"), null, "原值"));
        assertThat(enriched.get("empty")).isNull();
        verify(source).records(null, "", List.of("u"));
    }

    @Test void recordLoadsAreDeduplicatedAndSplitIntoBoundedBatches() {
        var source = userSource();
        List<List<String>> batches = new ArrayList<>();
        when(source.records(isNull(), eq(""), anyList())).thenAnswer(call -> {
            List<String> ids = call.getArgument(2);
            batches.add(List.copyOf(ids));
            Map<String, UserLabel> rows = new LinkedHashMap<>();
            for (String id : ids) rows.put(id, new UserLabel(id, "姓名" + id, true));
            return rows;
        });
        var input = new ArrayList<Name>();
        for (int index = 0; index < 401; index++) input.add(new Name("u" + index, null));
        input.add(new Name("u0", null));

        var result = (List<?>) new CachedResponseValues(List.of(source)).enrich(input);
        assertThat(batches).hasSize(3);
        assertThat(batches.get(0)).hasSize(200);
        assertThat(batches.get(1)).hasSize(200);
        assertThat(batches.get(2)).containsExactly("u400");
        assertThat(result).hasSize(402);
        assertThat(result.getFirst()).isEqualTo(new Name("u0", "姓名u0"));
        assertThat(result.get(400)).isEqualTo(new Name("u400", "姓名u400"));
        assertThat(result.getLast()).isEqualTo(result.getFirst());
    }

    @Test void explicitScopesAreIsolatedAndMissingScopesNeverLoad() {
        var source = userSource();
        when(source.scope()).thenReturn(CacheScope.UNIT);
        when(source.records("a", "", List.of("u"))).thenReturn(Map.of("u", new UserLabel("u", "A", true)));
        when(source.records("b", "", List.of("u"))).thenReturn(Map.of("u", new UserLabel("u", "B", true)));
        var processor = new CachedResponseValues(List.of(source));

        var result = processor.enrich(List.of(new UnitName("u", "a", null), new UnitName("u", "b", null),
                new UnitName("u", null, "旧值"), new UnitName("u", " ", "旧值")));
        assertThat(result).isEqualTo(List.of(new UnitName("u", "a", "A"), new UnitName("u", "b", "B"),
                new UnitName("u", null, null), new UnitName("u", " ", null)));
        verify(source).records("a", "", List.of("u"));
        verify(source).records("b", "", List.of("u"));
        verify(source, times(2)).records(any(), anyString(), anyList());
    }

    @Test void unitCacheWithoutExplicitScopeRequiresRequestIdentity() {
        var source = userSource();
        when(source.scope()).thenReturn(CacheScope.UNIT);
        var processor = new CachedResponseValues(List.of(source));
        assertThatThrownBy(() -> processor.enrich(new Name("u", null))).isInstanceOf(RuntimeException.class);
        verify(source, never()).records(any(), anyString(), anyList());
        ScopeContext.set(new RequestIdentity("actor", "a", List.of(), List.of()));
        when(source.records("a", "", List.of("u"))).thenReturn(Map.of("u", new UserLabel("u", "A", true)));
        assertThat(processor.enrich(new Name("u", null))).isEqualTo(new Name("u", "A"));
    }

    @Test void traversalLimitsFailBeforeLoadingAnyCache() {
        var source = userSource();
        var processor = new CachedResponseValues(List.of(source));
        Object nested = new Name("u", null);
        for (int depth = 0; depth < 22; depth++) nested = List.of(nested);
        Object tooDeep = nested;
        assertThatThrownBy(() -> processor.enrich(tooDeep)).hasMessageContaining("结构上限");
        assertThatThrownBy(() -> processor.enrich(Collections.nCopies(30_001, null))).hasMessageContaining("结构上限");
        verify(source, never()).records(any(), anyString(), anyList());
    }
}
