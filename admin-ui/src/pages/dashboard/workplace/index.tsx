import { PageContainer } from '@/components/ManagementLayout';
import { UserOutlined } from '@ant-design/icons';

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

export default function Workplace() {
  const { styles } = useStyles();
  const { initialState } = useModel('@@initialState');
  const access = useAccess();
  const user = initialState?.user;
  if (!user) return <Skeleton active />;
  const scope = user.scopes.find((item) => item.id === user.currentScopeId)?.name;
  const links = access.canReadUsers
    ? [{ title: '用户管理', href: '/system/users' }]
    : [];
  return (
    <PageContainer>
      <Card className={styles.summary}>
        <div className={styles.summaryContent}>
          <Avatar size={40} icon={<UserOutlined />} />
          <div className={styles.content}>
            <div className={styles.contentTitle}>你好，{user.displayName}</div>
            <div>{scope || '未选择'}</div>
          </div>
          <Statistic title="可访问机构" value={user.scopes.length} />
        </div>
      </Card>
      <Row gutter={[12, 12]}>
        <Col xl={16} lg={24} md={24} sm={24} xs={24}>
          <Card
            title="当前身份"
          >
            <Descriptions
              size="small"
              column={{ xs: 1, sm: 2, md: 2 }}
              items={[
                { key: 'account', label: '登录账号', children: user.loginName },
                { key: 'name', label: '姓名', children: user.displayName },
                {
                  key: 'scope',
                  label: '当前机构',
                  children: scope || '未选择',
                },
              ]}
            />
          </Card>
        </Col>
        <Col xl={8} lg={24} md={24} sm={24} xs={24}>
          <Card
            title="快捷入口"
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
