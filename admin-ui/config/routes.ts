export default [
  { path: '/login', redirect: '/user/login', layout: false },
  { path: '/user/login', component: './user/Login', layout: false },
  {
    path: '/workspace',
    name: '工作台',
    icon: 'DashboardOutlined',
    component: './dashboard/workplace',
  },
  {
    path: '/system/users',
    name: '用户管理',
    icon: 'TeamOutlined',
    access: 'canReadUsers',
    component: './table-list',
  },
  {
    path: '/system/departments',
    name: '部门管理',
    icon: 'ApartmentOutlined',
    access: 'canReadDepartments',
    component: './system/departments',
  },
  {
    path: '/system/roles',
    name: '角色管理',
    icon: 'SafetyCertificateOutlined',
    access: 'canReadRoles',
    component: './system/roles',
  },
  { path: '/', redirect: '/workspace' },
  { path: '*', component: './exception/404', hideInMenu: true },
];
