package com.cyys.common.cache;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.R;
import org.springframework.stereotype.Component;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/** 响应序列化前批量取记录缓存；无启动扫描，支持记录/列表缓存，不处理历史快照。 */
@Component
public class CachedResponseValues {
    private static final int BATCH_SIZE = 200;
    private static final int MAX_DEPTH = 20;
    private static final int MAX_NODES = 30_000;
    private final Map<String, CacheValueSource<?>> sources;
    private final ClassValue<Schema> schemas = new ClassValue<>() {
        @Override protected Schema computeValue(Class<?> type) {
            var fields = type.getRecordComponents();
            try {
                return new Schema(fields, type.getConstructor(Arrays.stream(fields).map(RecordComponent::getType).toArray(Class<?>[]::new)));
            } catch (NoSuchMethodException failure) { throw new IllegalStateException("响应 record 必须有公开构造器", failure); }
        }
    };

    public CachedResponseValues(List<CacheValueSource<?>> providers) {
        Map<String, CacheValueSource<?>> registered = new LinkedHashMap<>();
        for (var source : providers) {
            if (registered.putIfAbsent(source.name(), source) != null)
                throw new IllegalStateException("重复登记的记录缓存：" + source.name());
        }
        sources = Map.copyOf(registered);
    }

    public Object enrich(Object response) {
        var work = new Work();
        Supplier<Object> result = work.collect(response, 0);
        work.load();
        return result.get();
    }

    private final class Work {
        private int nodes;
        private final Map<Batch, LinkedHashSet<String>> requests = new LinkedHashMap<>();
        private final Map<Batch, Map<String, ? extends Record>> loaded = new HashMap<>();
        private final Map<Batch, List<? extends Record>> lists = new HashMap<>();

        @SuppressWarnings({"rawtypes", "unchecked"})
        Supplier<Object> collect(Object value, int depth) {
            if (++nodes > MAX_NODES || depth > MAX_DEPTH) throw new IllegalStateException("缓存响应回填超过结构上限");
            if (value == null) return () -> null;
            if (value instanceof R<?> envelope) {
                if (!Integer.valueOf(200).equals(envelope.getCode())) return () -> value;
                var data = collect(envelope.getData(), depth+1);
                return () -> { R result = new R(); result.setCode(envelope.getCode()); result.setMsg(envelope.getMsg());
                    result.setTimestamp(envelope.getTimestamp()); result.setData(data.get()); return result; };
            }
            if (value instanceof IPage page) {
                var records = collect(page.getRecords(), depth+1);
                return () -> { page.setRecords((List) records.get()); return page; };
            }
            if (value instanceof List<?> list) {
                var children = list.stream().map(child -> collect(child, depth+1)).toList();
                return () -> children.stream().map(Supplier::get).toList();
            }
            if (value instanceof Map<?,?> map) {
                Map<Object,Supplier<Object>> children = new LinkedHashMap<>();
                map.forEach((key, child) -> children.put(key, collect(child, depth+1)));
                return () -> { Map<Object,Object> result = new LinkedHashMap<>(); children.forEach((key, child) -> result.put(key, child.get())); return result; };
            }
            if (!value.getClass().isRecord()) return () -> value;
            Schema schema = schemas.get(value.getClass());
            Object[] original = Arrays.stream(schema.fields).map(field -> read(field.getAccessor(), value)).toArray();
            List<Supplier<Object>> children = new ArrayList<>();
            for (int index=0; index<schema.fields.length; index++) {
                var field = schema.fields[index];
                var annotation = field.getAnnotation(CacheValue.class);
                if (annotation == null) { children.add(collect(original[index], depth+1)); continue; }
                var source = sources.get(annotation.cache());
                if (source != null) checkType(field, source, annotation);
                String id = scalar(property(schema, original, annotation.key()));
                if (source == null || ((!annotation.key().isEmpty() || source.shape() == CacheShape.RECORD) && (id == null || id.isBlank()))) { children.add(() -> empty(field.getType())); continue; }
                String scope = source.scope() == CacheScope.SYSTEM ? null
                        : annotation.scopeKey().isEmpty() ? ScopeContext.get().requireScopeId()
                        : scalar(property(schema, original, annotation.scopeKey()));
                // 缺少真实单位不能变成全系统查询。
                if (source.scope() == CacheScope.UNIT && (scope == null || scope.isBlank())) { children.add(() -> empty(field.getType())); continue; }
                var batch = new Batch(source.name(), scope, annotation.variant());
                var ids = requests.computeIfAbsent(batch, ignored -> new LinkedHashSet<>());
                if (id != null) ids.add(id);
                children.add(() -> {
                    if (annotation.key().isEmpty()) return annotation.field().isEmpty() ? lists.get(batch) : null;
                    Record row = loaded.get(batch).get(id);
                    Object selected = annotation.field().isEmpty() ? row : recordField(row, annotation.field());
                    return selected == null ? empty(field.getType()) : selected;
                });
            }
            return () -> {
                try { return schema.constructor.newInstance(children.stream().map(Supplier::get).toArray()); }
                catch (ReflectiveOperationException failure) { throw new IllegalStateException("响应 DTO 构造失败", failure); }
            };
        }

        void load() {
            requests.forEach((batch, ids) -> {
                Map<String,Record> result = new LinkedHashMap<>(); var unique = List.copyOf(ids);
                var source = sources.get(batch.source);
                if (source.shape() == CacheShape.LIST) {
                    var rows = source.list(batch.scope, batch.variant);
                    lists.put(batch, rows);
                    for (Record row : rows) {
                        String id = scalar(recordField(row, source.listKey()));
                        if (id != null) result.putIfAbsent(id, row);
                    }
                }
                else for (int from=0; from<unique.size(); from+=BATCH_SIZE)
                    result.putAll(sources.get(batch.source).records(batch.scope, batch.variant, unique.subList(from, Math.min(from+BATCH_SIZE, unique.size()))));
                loaded.put(batch, result);
            });
        }
    }

    private static Object property(Schema schema, Object[] values, String name) {
        for (int i=0; i<schema.fields.length; i++) if (schema.fields[i].getName().equals(name)) return values[i];
        return null;
    }
    private static Object recordField(Record row, String name) {
        if (row != null) for (var field : row.getClass().getRecordComponents())
            if (field.getName().equals(name)) return read(field.getAccessor(), row);
        return null;
    }
    private static Object read(Method accessor, Object value) {
        try { return accessor.invoke(value); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("响应 DTO 字段读取失败", failure); }
    }
    private static String scalar(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Enum<?> ? value.toString() : null;
    }
    /** 只比较声明类型，与实际值是否为空无关；不进行全项目启动扫描。 */
    private static void checkType(RecordComponent target, CacheValueSource<?> source, CacheValue annotation) {
        Type supplied = source.recordType();
        if (source.shape() == CacheShape.LIST && annotation.key().isEmpty() && annotation.field().isEmpty()) {
            if (!(target.getGenericType() instanceof ParameterizedType list)
                    || !List.class.equals(list.getRawType())
                    || !accepts(list.getActualTypeArguments()[0], supplied)) throw typeMismatch(target, source);
            return;
        }
        if (!annotation.field().isEmpty()) {
            var matched = Arrays.stream(source.recordType().getRecordComponents())
                    .filter(field -> field.getName().equals(annotation.field())).findFirst();
            if (matched.isEmpty()) return; // 不存在的缓存字段没有值，也没有可比较的字段类型。
            supplied = matched.get().getGenericType();
        }
        if (!accepts(target.getGenericType(), supplied)) throw typeMismatch(target, source);
    }
    private static boolean accepts(Type target, Type supplied) {
        if (target instanceof Class<?> to && supplied instanceof Class<?> from)
            return boxed(to).isAssignableFrom(boxed(from));
        return target.equals(supplied);
    }
    private static Class<?> boxed(Class<?> type) {
        return type.isPrimitive() ? empty(type).getClass() : type;
    }
    private static IllegalStateException typeMismatch(RecordComponent target, CacheValueSource<?> source) {
        return new IllegalStateException("缓存字段类型不匹配：" + target.getDeclaringRecord().getSimpleName()
                + "." + target.getName() + "，来源 " + source.name());
    }
    private static Object empty(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte)0;
        if (type == short.class) return (short)0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
    private record Schema(RecordComponent[] fields, Constructor<?> constructor) {}
    private record Batch(String source, String scope, String variant) {}
}
