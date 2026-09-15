import { UserOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { Link, useAccess, useModel } from '@umijs/max';
import {
  Avatar,
  Card,
  Col,
  Descriptions,
  Empty,
  Row,
  Skeleton,
  Statistic,
} from 'antd';
import EditableLinkGroup from './components/EditableLinkGroup';
import useStyles from './style.style';
import { session } from '@/utils/session';

export default function Workplace() {
  const { styles } = useStyles();
  const { initialState } = useModel('@@initialState');
  const access = useAccess();
  const user = initialState?.user;
  if (!user) return <Skeleton active />;
  const scope = session().allUnits
    ? '全部单位 · 只读'
    : user.scopes.find((item) => item.id === user.currentScopeId)?.name;
  const links = access.canReadUsers
    ? [{ title: '用户管理', href: '/system/users' }]
    : [];
  return (
    <PageContainer
      content={
        <div className={styles.pageHeaderContent}>
          <div className={styles.avatar}>
            <Avatar size="large" icon={<UserOutlined />} />
          </div>
          <div className={styles.content}>
            <div className={styles.contentTitle}>你好，{user.displayName}</div>
            <div>{scope || '请选择工作单位'}</div>
          </div>
        </div>
      }
      extraContent={
        <div className={styles.extraContent}>
          <div className={styles.statItem}>
            <Statistic title="可访问单位" value={user.scopes.length} />
          </div>
        </div>
      }
    >
      <Row gutter={24}>
        <Col xl={16} lg={24} md={24} sm={24} xs={24}>
          <Card
            className={styles.projectList}
            title="当前身份"
            variant="borderless"
            style={{ marginBottom: 24 }}
          >
            <Descriptions
              column={1}
              items={[
                { key: 'account', label: '登录账号', children: user.loginName },
                { key: 'name', label: '姓名', children: user.displayName },
                {
                  key: 'scope',
                  label: '工作单位',
                  children: scope || '未选择',
                },
              ]}
            />
          </Card>
        </Col>
        <Col xl={8} lg={24} md={24} sm={24} xs={24}>
          <Card
            title="快速开始 / 便捷导航"
            variant="borderless"
            style={{ marginBottom: 24 }}
          >
            {links.length ? (
              <EditableLinkGroup links={links} linkElement={Link} />
            ) : (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无可用管理入口"
              />
            )}
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
}
