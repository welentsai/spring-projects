import React from 'react';
import { Layout } from 'antd';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Sidebar from './Sidebar';
import Home from '../pages/Home';
import Profile from '../pages/Profile';
import Settings from '../pages/Settings';
import CityList from './CityList';

const { Sider, Content: AntContent } = Layout;

const AppLayout: React.FC = () => {
    return (
        <Router>
            <Layout className="min-h-screen">
                <Sider width={200} className="bg-white">
                    <Sidebar />
                </Sider>
                <AntContent className="p-6">
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
};

export default AppLayout;