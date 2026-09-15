import packageJson from '../../../package.json';
import { createStyles } from 'antd-style';
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
  const year = new Date().getFullYear();

  return (
    <div className={styles.footer}>
      <div className={styles.copyright}>CYYS &copy; {year}</div>
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
