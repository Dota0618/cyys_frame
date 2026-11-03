import { createStyles } from 'antd-style';
import { useModel } from '@umijs/max';
import React from 'react';

const useStyles = createStyles(({ token, css }) => ({
  footer: css`
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 4px 12px;
    padding: 8px 12px;
    text-align: center;
    color: ${token.colorTextDescription};
    font-size: ${token.fontSizeSM}px;
    line-height: ${token.lineHeight};
  `,
}));

const Footer: React.FC = () => {
  const { styles } = useStyles();
  const { initialState } = useModel('@@initialState');
  const year = new Date().getFullYear();
  const systemName = initialState?.publicConfigurations?.SYSTEM_NAME?.trim() || '';
  const copyright = initialState?.publicConfigurations?.COPYRIGHT_TEXT?.trim()
    || (systemName ? `${systemName} © ${year}` : `© ${year}`);
  const contactPhone = initialState?.publicConfigurations?.CONTACT_PHONE?.trim();

  return (
    <div className={styles.footer}>
      <span>{copyright}</span>
      {contactPhone && <span>联系电话：{contactPhone}</span>}
    </div>
  );
};

export default Footer;
