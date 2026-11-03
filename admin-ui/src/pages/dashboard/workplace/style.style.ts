import { createStyles } from 'antd-style';

const useStyles = createStyles(({ token }) => ({
  summary: { marginBottom: 12 },
  summaryContent: {
    display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 12,
  },
  content: {
    flex: '1 1 180px', minWidth: 0, color: token.colorTextSecondary,
    lineHeight: '22px', overflowWrap: 'anywhere',
  },
  contentTitle: {
    marginBottom: 4, color: token.colorTextHeading, fontWeight: 500,
    fontSize: 16, lineHeight: '24px',
  },
}));

export default useStyles;
