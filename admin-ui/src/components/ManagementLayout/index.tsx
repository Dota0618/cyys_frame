import type { FormItemProps, FormProps, ModalProps } from 'antd';
import type { ReactNode } from 'react';
import styles from './index.less';

export function ManagementSplit({ navigation, children }: { navigation: ReactNode; children: ReactNode }) {
  return <div className={styles.split}><aside className={styles.navigation}>{navigation}</aside><div className={styles.content}>{children}</div></div>;
}

export function CompactFields({ children }: { children: ReactNode }) {
  return <div className={styles.fields}>{children}</div>;
}

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
