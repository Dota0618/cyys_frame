package com.cyys.application;

import com.cyys.admin.user.dto.UserLabel;
import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.common.cache.*;
import com.cyys.common.web.R;
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
    public record ListLookup(String code,
            @CacheValue(cache="options", key="code", field="label") String label,
            @CacheValue(cache="options", key="code") DictionaryOption option,
            @CacheValue(cache="options") List<DictionaryOption> all) {}

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
}
