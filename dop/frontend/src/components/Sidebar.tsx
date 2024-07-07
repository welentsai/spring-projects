import React from 'react';
import { Menu } from 'antd';
import { Link, useLocation } from 'react-router-dom';
import { HomeOutlined, UserOutlined, SettingOutlined, GlobalOutlined } from '@ant-design/icons';

const Sidebar: React.FC = () => {
    const location = useLocation();

    return (
        <Menu mode="inline" selectedKeys={[location.pathname]}>
            <Menu.Item key="/" icon={<HomeOutlined />}>
                <Link to="/">Home</Link>
            </Menu.Item>
            <Menu.Item key="/profile" icon={<UserOutlined />}>
                <Link to="/profile">Profile</Link>
            </Menu.Item>
            <Menu.Item key="/settings" icon={<SettingOutlined />}>
                <Link to="/settings">Settings</Link>
            </Menu.Item>
            <Menu.Item key="/cities" icon={<GlobalOutlined />}>
                <Link to="/cities">Cities</Link>
            </Menu.Item>
        </Menu>
    );
};

export default Sidebar;