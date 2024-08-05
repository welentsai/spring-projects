import React, { useState, useEffect } from 'react';
import { List, Spin } from 'antd';
import { cityService, City } from '../../services/CityService';

const CityList: React.FC = () => {
    const [cities, setCities] = useState<City[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchCities = async () => {
            try {
                setLoading(true);
                const fetchedCities = await cityService.getAllCities();
                setCities(fetchedCities);
                setLoading(false);
            } catch (err) {
                setError('Failed to fetch cities');
                setLoading(false);
            }
        };

        fetchCities();
    }, []);

    if (loading) {
        return (
            <div className="flex justify-center items-center h-full">
                <Spin size="large" />
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex justify-center items-center h-full">
                <div className="text-red-500">{error}</div>
            </div>
        );
    }

    return (
        <List
            className="bg-white rounded-lg shadow"
            header={<div className="text-xl font-bold">Cities</div>}
            bordered
            dataSource={cities}
            renderItem={(city) => (
                <List.Item>
                    <List.Item.Meta
                        title={<span className="font-semibold">{city.name}</span>}
                        description={`City ID: ${city.id}`}
                    />
                </List.Item>
            )}
        />
    );
};

export { CityList };