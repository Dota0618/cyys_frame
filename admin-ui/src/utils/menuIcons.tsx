import {
  AppstoreOutlined, ApartmentOutlined, AuditOutlined, BankOutlined, BellOutlined,
  BookOutlined, CalendarOutlined, CloudOutlined, CodeOutlined, CrownOutlined,
  DashboardOutlined, DatabaseOutlined, DeleteOutlined, DesktopOutlined, FileOutlined,
  FileTextOutlined, FolderOutlined, GlobalOutlined, HomeOutlined, IdcardOutlined,
  KeyOutlined, LinkOutlined, LockOutlined, LoginOutlined, MailOutlined, MenuOutlined,
  NotificationOutlined, PictureOutlined, SafetyCertificateOutlined, SettingOutlined,
  ShopOutlined, SolutionOutlined, TableOutlined, TagsOutlined, TeamOutlined,
  ToolOutlined, UserOutlined, VideoCameraOutlined, HistoryOutlined, SearchOutlined,
} from '@ant-design/icons';

const icons = {
  AppstoreOutlined, ApartmentOutlined, AuditOutlined, BankOutlined, BellOutlined,
  BookOutlined, CalendarOutlined, CloudOutlined, CodeOutlined, CrownOutlined,
  DashboardOutlined, DatabaseOutlined, DeleteOutlined, DesktopOutlined, FileOutlined,
  FileTextOutlined, FolderOutlined, GlobalOutlined, HomeOutlined, IdcardOutlined,
  KeyOutlined, LinkOutlined, LockOutlined, LoginOutlined, MailOutlined, MenuOutlined,
  NotificationOutlined, PictureOutlined, SafetyCertificateOutlined, SettingOutlined,
  ShopOutlined, SolutionOutlined, TableOutlined, TagsOutlined, TeamOutlined,
  ToolOutlined, UserOutlined, VideoCameraOutlined, HistoryOutlined, SearchOutlined,
};
const labels = ['应用', '部门', '审计', '单位', '提醒', '字典', '日历', '云服务', '代码', '平台角色',
  '工作台', '数据库', '回收站', '桌面', '文件', '文档', '文件夹', '地区', '首页', '身份',
  '密钥', '链接', '锁定', '登录', '邮件', '菜单', '通知', '图片', '安全', '设置',
  '商店', '档案', '表格', '标签', '用户组', '工具', '用户', '视频', '历史', '搜索'];

export function menuIcon(name?: string) {
  const Icon = name && Object.hasOwn(icons, name) ? icons[name as keyof typeof icons] : undefined;
  return Icon ? <Icon /> : undefined;
}

export const menuIconOptions = Object.keys(icons).map((value, index) => ({
  value, search: `${labels[index]} ${value}`,
  label: <span style={{ display: 'inline-flex', gap: 10, alignItems: 'center' }}>{menuIcon(value)}{labels[index]}</span>,
}));
