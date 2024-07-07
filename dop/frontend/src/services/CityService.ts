import axios from 'axios';

const API_BASE_URL = '/api'; // Replace with your actual API base URL

export interface City {
    id: number;
    name: string;
    country: string;
}

export const cityService = {
    getAllCities: async (): Promise<City[]> => {
        try {
            const response = await axios.get<City[]>(`${API_BASE_URL}/api/v1/cities`);
            return response.data;
        } catch (error) {
            console.error('Error fetching cities:', error);
            throw error;
        }
    },

    getCityById: async (id: number): Promise<City> => {
        try {
            const response = await axios.get<City>(`${API_BASE_URL}/api/v1/cities/${id}`);
            return response.data;
        } catch (error) {
            console.error(`Error fetching city with id ${id}:`, error);
            throw error;
        }
    },

    createCity: async (city: Omit<City, 'id'>): Promise<City> => {
        try {
            const response = await axios.post<City>(`${API_BASE_URL}/api/v1/cities`, city);
            return response.data;
        } catch (error) {
            console.error('Error creating city:', error);
            throw error;
        }
    },

    updateCity: async (id: number, city: Partial<City>): Promise<City> => {
        try {
            const response = await axios.put<City>(`${API_BASE_URL}/api/v1/cities/${id}`, city);
            return response.data;
        } catch (error) {
            console.error(`Error updating city with id ${id}:`, error);
            throw error;
        }
    },

    deleteCity: async (id: number): Promise<void> => {
        try {
            await axios.delete(`${API_BASE_URL}/api/v1/cities/${id}`);
        } catch (error) {
            console.error(`Error deleting city with id ${id}:`, error);
            throw error;
        }
    }
};