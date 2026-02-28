import React from 'react';
import { Sidebar, Menu, MenuItem, useProSidebar } from 'react-pro-sidebar';
import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaBook, FaUsers, FaLayerGroup, FaBars } from 'react-icons/fa';

const AppSidebar = () => {
  const { t } = useTranslation();
  const { collapseSidebar, collapsed } = useProSidebar();
  const location = useLocation();

  const menuItemStyles = {
    root: {
      fontSize: '1rem',
      fontWeight: 400,
    },
    icon: {
      color: '#3b82f6', // blue-500
    },
    button: {
      '&:hover': {
        backgroundColor: '#eff6ff', // blue-50
        color: '#1d4ed8', // blue-700
      },
    },
  };

  const getActiveStyle = (path) => {
    return location.pathname === path ? {
      backgroundColor: '#dbeafe', // blue-200
      color: '#1e3a8a', // blue-800
    } : {};
  };

  return (
    <Sidebar backgroundColor="#fff" rootStyles={{ borderRight: '1px solid #e5e7eb' }}>
      <Menu menuItemStyles={menuItemStyles}>
        <MenuItem
          icon={<FaBars />}
          onClick={() => collapseSidebar()}
          style={{ textAlign: 'center', fontWeight: 'bold', fontSize: '1.25rem', color: '#111827' }}
        >
          {!collapsed && t('header.title')}
        </MenuItem>

        <MenuItem
          component={<Link to="/" />}
          icon={<FaBook />}
          style={getActiveStyle('/')}
        >
          {t('header.books')}
        </MenuItem>
        <MenuItem
          component={<Link to="/authors" />} 
          icon={<FaUsers />}
          style={getActiveStyle('/authors')}
        >
          {t('header.authors')}
        </MenuItem>
        <MenuItem
          component={<Link to="/series" />} 
          icon={<FaLayerGroup />}
          style={getActiveStyle('/series')}
        >
          {t('header.series')}
        </MenuItem>
      </Menu>
    </Sidebar>
  );
};

export default AppSidebar;
