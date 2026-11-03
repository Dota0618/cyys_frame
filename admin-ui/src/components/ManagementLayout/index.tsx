import type { FormItemProps, FormProps, ModalFuncProps, ModalProps } from 'antd';
import { Card, Space, Typography } from 'antd';
import type { ProTableProps } from '@ant-design/pro-components';
import type { ReactNode } from 'react';
import styles from './index.less';

export { default as PageContainer } from '../PageContainer';
export { default as PageTable } from '../PageTabs/PageTable';
export { usePageState } from '../PageTabs/context';

export function ManagementSplit({ navigation, children }: { navigation: ReactNode; children: ReactNode }) {
  return <div className={styles.split}><aside className={styles.navigation}>{navigation}</aside><div className={styles.content}>{children}</div></div>;
}

export function CompactFields({ children }: { children: ReactNode }) {
  return <div className={styles.fields}>{children}</div>;
}

export function ListFilters({ children, actions, description }: { children: ReactNode; actions?: ReactNode; description?: ReactNode }) {
  return <Card size="small" className={styles.filters}>
    <div className={styles.filterFields}>
      {children}
      {actions && <Space wrap className={styles.filterActions}>{actions}</Space>}
    </div>
    {description && <div className={styles.filterDescription}><Typography.Text type="secondary">{description}</Typography.Text></div>}
  </Card>;
}

export function FilterField({ label, htmlFor, children }: { label: string; htmlFor: string; children: ReactNode }) {
  return <div className={styles.filterField}><label htmlFor={htmlFor}>{label}</label><div className={styles.filterControl}>{children}</div></div>;
}

export const tableSearchProps = {
  labelWidth: 80, defaultCollapsed: false, searchText: '查询', resetText: '重置',
  span: { xs: 24, sm: 12, md: 12, lg: 8, xl: 6, xxl: 6 }, searchGutter: 12,
} satisfies Exclude<ProTableProps<Record<string, unknown>, Record<string, unknown>>['search'], boolean | undefined>;

export const managementTableProps = {
  size: 'small' as const, search: tableSearchProps, scroll: { x: 'max-content' }, cardBordered: true,
};

/** Shared edit-form layout; wide controls opt into verticalFormItemProps. */
export const compactFormProps: Pick<FormProps, 'layout' | 'labelCol' | 'wrapperCol' | 'labelAlign' | 'labelWrap' | 'className'> = {
  layout: 'horizontal', labelCol: { flex: '104px' }, wrapperCol: { flex: '1 1 0' },
  labelAlign: 'left', labelWrap: true, className: styles.form,
};

export const verticalFormItemProps: Pick<FormItemProps, 'layout' | 'labelCol' | 'wrapperCol'> = {
  layout: 'vertical', labelCol: { flex: 'none' }, wrapperCol: { flex: 'none' },
};

/** Keep the form body scrollable while its title and submit buttons stay visible. */
export const compactModalProps: ModalProps = {
  centered: true, destroyOnHidden: true, className: styles.compactModal,
  styles: {
    container: { maxHeight: 'calc(100dvh - 32px)', display: 'flex', flexDirection: 'column' },
    body: { minHeight: 0, overflowY: 'auto', overflowX: 'hidden', paddingRight: 4 },
    header: { flexShrink: 0 }, footer: { flexShrink: 0, marginTop: 16 },
  },
};

export const confirmModalProps: ModalFuncProps = { ...compactModalProps, width: 560, okText: '确认', cancelText: '取消' };
