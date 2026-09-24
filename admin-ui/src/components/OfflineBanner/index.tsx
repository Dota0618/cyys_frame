import { getIntl, useModel } from '@umijs/max';
import { Alert } from 'antd';
import { useEffect, useState } from 'react';

const OfflineBanner: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const [isOnline, setIsOnline] = useState(
    typeof navigator !== 'undefined' ? navigator.onLine : true,
  );

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const maintenance = initialState?.publicConfigurations?.MAINTENANCE_MODE === '1';
  if (isOnline && !maintenance) return null;

  return <>
    {!isOnline && <Alert
      type="warning"
      showIcon
      closable={false}
      style={{ position: 'fixed', top: 8, left: '50%', transform: 'translateX(-50%)', zIndex: 999, maxWidth: 480 }}
      title={getIntl().formatMessage({
        id: 'app.network.offline',
        defaultMessage: 'You are currently offline. Some features may be unavailable.',
      })}
    />}
    {maintenance && <Alert
      type="warning"
      banner
      showIcon
      closable={false}
      title="系统维护中，普通账号暂时无法登录。"
    />}
  </>;
};

export default OfflineBanner;
