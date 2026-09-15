import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { LoginForm, ProFormText } from '@ant-design/pro-components';
import { Helmet, SelectLang, history, useModel } from '@umijs/max';
import { Alert, App, Tabs } from 'antd';
import { createStyles } from 'antd-style';
import { useState } from 'react';
import { Footer } from '@/components';
import { login } from '@/services/auth';
import { ApiError, StaleRequest } from '@/utils/request';
import { session, updateSession } from '@/utils/session';

const useStyles = createStyles(({ token }) => ({
  lang: {
    width: 42,
    height: 42,
    lineHeight: '42px',
    position: 'fixed',
    right: 16,
    borderRadius: token.borderRadius,
    ':hover': { backgroundColor: token.colorBgTextHover },
  },
  container: {
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    overflow: 'auto',
    backgroundImage:
      "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
    backgroundSize: '100% 100%',
  },
}));

const safeRedirect = () => {
  const value = new URLSearchParams(window.location.search).get('redirect');
  if (!value?.startsWith('/') || value.startsWith('//')) return '/workspace';
  try {
    const url = new URL(value, window.location.origin);
    if (
      url.origin !== window.location.origin ||
      ['/login', '/user/login'].includes(url.pathname)
    )
      return '/workspace';
    return url.pathname + url.search + url.hash;
  } catch {
    return '/workspace';
  }
};

export default function Login() {
  const { setInitialState } = useModel('@@initialState');
  const { styles } = useStyles();
  const { message } = App.useApp();
  const [error, setError] = useState('');
  return (
    <div className={styles.container}>
      <Helmet>
        <title>登录 - CYYS</title>
      </Helmet>
      <div className={styles.lang} data-lang>
        <SelectLang />
      </div>
      <div style={{ flex: '1', padding: '32px 0' }}>
        <LoginForm<{ loginName: string; password: string }>
          name="cyys-login"
          contentStyle={{ minWidth: 280, maxWidth: '75vw' }}
          logo={<img alt="logo" src="/logo.svg" />}
          title="CYYS"
          subTitle="CYYS 管理平台"
          onFinish={async (values) => {
            setError('');
            try {
              const result = await login(values);
              const redirect = safeRedirect();
              updateSession({
                token: result.token,
                scopeId: result.userInfo.currentScopeId,
              });
              await setInitialState((s) => ({
                ...s,
                user: result.userInfo,
                revision: session().revision,
                switching: false,
              }));
              void message.success('登录成功');
              history.replace(redirect);
            } catch (reason) {
              if (!(reason instanceof StaleRequest))
                setError(
                  reason instanceof ApiError
                    ? reason.message
                    : '登录失败，请重试',
                );
            }
          }}
        >
          <Tabs
            centered
            activeKey="account"
            items={[{ key: 'account', label: '账户密码登录' }]}
          />
          {error && (
            <Alert
              title={error}
              type="error"
              showIcon
              style={{ marginBottom: 24 }}
            />
          )}
          <ProFormText
            name="loginName"
            placeholder="请输入账号"
            fieldProps={{
              size: 'large',
              prefix: <UserOutlined />,
              'aria-label': '登录账号',
              autoComplete: 'username',
            }}
            rules={[{ required: true, message: '请输入账号' }]}
          />
          <ProFormText.Password
            name="password"
            placeholder="请输入密码"
            fieldProps={{
              size: 'large',
              prefix: <LockOutlined />,
              'aria-label': '密码',
              autoComplete: 'current-password',
            }}
            rules={[{ required: true, message: '请输入密码' }]}
          />
        </LoginForm>
      </div>
      <Footer />
    </div>
  );
}
