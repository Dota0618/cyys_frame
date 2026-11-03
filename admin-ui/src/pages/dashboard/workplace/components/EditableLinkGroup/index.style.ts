import { createStyles } from 'antd-style';

const useStyles = createStyles(({ token }) => ({
  linkGroup: {
    display: 'flex', flexWrap: 'wrap', gap: '8px 12px',
    '& > a': {
      display: 'inline-flex', alignItems: 'center', minHeight: 32,
      padding: '4px 8px', borderRadius: token.borderRadius,
      color: token.colorText, fontSize: token.fontSize, lineHeight: '22px',
      '&:hover': { color: token.colorPrimary, background: token.colorPrimaryBg },
    },
  },
}));

export default useStyles;
