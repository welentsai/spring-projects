import React from 'react';
import { Layout } from 'antd';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Home from '../../../pages/Home';
import Profile from '../../../pages/Profile';
import Settings from '../../../pages/Settings';
import { Sidebar } from '../SideBar';
import { CityList } from '../../feature/CityList';

const { Sider, Content: AntContent } = Layout;

const AppLayout = () =>
(
    <Router>
        <Layout className="min-h-screen">
            <Sider width={200} className="bg-white">
                <Sidebar />
            </Sider>
            <AntContent className="p-6 h-[calc(100vh-64px)]">
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/profile" element={<Profile />} />
                    <Route path="/settings" element={<Settings />} />
                    <Route path="/cities" element={<CityList />} />
                </Routes>
            </AntContent>
        </Layout>
    </Router>
);

export { AppLayout };