import { ProTable } from '@ant-design/pro-components';
import type { ProFormInstance, ProTableProps } from '@ant-design/pro-components';
import { useContext, useRef } from 'react';
import { PageStateContext } from './context';

interface TableState {
  fields?: Record<string, unknown>;
  query?: Record<string, unknown>;
  current?: number;
  pageSize?: number;
}

/** 在原 ProTable 上恢复查询与分页，仍由页面重新请求列表。 */
export default function PageTable<Data extends Record<string, any>, Params extends Record<string, any> = Record<string, any>, ValueType = 'text'>(
  { rememberState = true, stateKey = '', ...props }: ProTableProps<Data, Params, ValueType> & { rememberState?: boolean; stateKey?: string },
) {
  const context = useContext(PageStateContext);
  const formRef = useRef<ProFormInstance | undefined>(undefined);
  const initializing = useRef(!props.manualRequest);
  const key = `table:${props.form?.name || 'list'}:${stateKey}`;
  const saved = useRef<TableState>(rememberState ? context?.read<TableState>(key) || {} : {}).current;
  const update = (value: Partial<TableState>) => {
    Object.assign(saved, value);
    if (rememberState) context?.write(key, { ...saved });
  };
  const pagination = props.pagination === false ? false : {
    ...props.pagination,
    defaultCurrent: saved.current ?? props.pagination?.defaultCurrent,
    defaultPageSize: saved.pageSize ?? props.pagination?.defaultPageSize,
  };

  return <ProTable<Data, Params, ValueType> {...props}
    formRef={props.formRef || formRef}
    pagination={pagination}
    beforeSearchSubmit={values => {
      const query = props.beforeSearchSubmit?.(values) || values;
      if (initializing.current) {
        initializing.current = false;
        const form = props.formRef?.current || formRef.current;
        if (saved.fields) form?.setFieldsValue(saved.fields);
        return { ...query, ...saved.query } as Params;
      }
      update({ query });
      return query as Params;
    }}
    onSubmit={values => {
      const form = props.formRef?.current || formRef.current;
      update({ fields: form?.getFieldsValue(true) || values, current: 1 });
      props.onSubmit?.(values);
    }}
    onReset={() => { update({ fields: {}, query: {}, current: 1 }); props.onReset?.(); }}
    onChange={(page, filters, sorter, extra) => {
      update({ current: page.current, pageSize: page.pageSize });
      props.onChange?.(page, filters, sorter, extra);
    }}
  />;
}
