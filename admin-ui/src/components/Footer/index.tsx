import packageJson from '../../../package.json';
import { createStyles } from 'antd-style';
import { useModel } from '@umijs/max';
import React from 'react';

const useStyles = createStyles(({ token, css }) => ({
  footer: css`
    padding: 16px 24px;
    text-align: center;
    color: ${token.colorTextDescription};
    font-size: ${token.fontSizeSM}px;
    line-height: ${token.lineHeight};
    background: transparent;
  `,
  copyright: css`
    margin-bottom: 6px;
  `,
  link: css`
    color: ${token.colorTextDescription};
    text-decoration: none;
    transition: color ${token.motionDurationMid};

    &:hover {
      color: ${token.colorText};
    }
  `,
  meta: css`
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 6px 12px;
    font-family: ${token.fontFamilyCode};
    font-size: ${token.fontSizeSM - 1}px;
  `,
  group: css`
    display: inline-flex;
    align-items: center;
    gap: 4px;
  `,
  label: css`
    color: ${token.colorTextQuaternary};
  `,
  divider: css`
    display: inline-block;
    vertical-align: middle;
  `,
}));

const Footer: React.FC = () => {
  const { styles } = useStyles();
  const { initialState } = useModel('@@initialState');
  const year = new Date().getFullYear();
  const systemName = initialState?.settings?.title || 'CYYS';
  const copyright = initialState?.publicConfigurations?.COPYRIGHT_TEXT?.trim()
    || `${systemName} © ${year}`;
  const contactPhone = initialState?.publicConfigurations?.CONTACT_PHONE?.trim();

  return (
    <div className={styles.footer}>
      <div className={styles.copyright}>{copyright}</div>
      {contactPhone && <div className={styles.meta}><span className={styles.group}>联系电话：{contactPhone}</span></div>}
      <a
        className={styles.link}
        href={packageJson.template.repository}
        target="_blank"
        rel="noopener noreferrer"
      >
        Ant Design Pro
      </a>
    </div>
  );
};

export default Footer;
