import { create } from 'zustand';
import { Order } from '../context/OrderContext';

interface OrderState {
    orders: Array<Order>
    setOrders: (orders: Array<Order>) => void
}

export const useOrderStore = create<OrderState>()((set) => ({
    orders: [],
    setOrders: (orders: Array<Order>) => set(() => ({ orders })),
    // increment: () => set((state) => ({ count: state.count + 1 })),
    // decrement: () => set((state) => ({ count: state.count - 1 })),
}));