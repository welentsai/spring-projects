import * as React from 'react';
import { Dessert } from '../components/DessertCard';

export type Order = {
    dessert: Dessert,
    quantity: number
}

type OrderContextType = {
    orders: Array<Order>,
    setOrders: (orders: Array<Order>) => void
}

const OrderContext = React.createContext<OrderContextType>({
    orders: [],
    setOrders: () => { },
});


interface OrderProviderProps {
    children: React.ReactNode
}

export const OrderProvider = ({ children }: OrderProviderProps) => {
    const [orders, setOrders] = React.useState<Array<Order>>([]);

    return (
        <OrderContext.Provider value={{ orders, setOrders }}>
            {children}
        </OrderContext.Provider>
    )
}

export const useOrder = () => {

    const { orders, setOrders } = React.useContext(OrderContext)

    const handleOrderChange = (newOrder: Order) => {
        let found = false;

        const newOrders = orders.map(order => {
            if (order.dessert.name === newOrder.dessert.name) {
                found = true;
                return { ...order, quantity: order.quantity + newOrder.quantity }
            }
            return order;
        }).filter((order) => order.quantity > 0);

        if (found === false && newOrder.quantity > 0) { // Add new order that not exist
            setOrders([...orders, newOrder]);
        } else {
            setOrders(newOrders);
        }

    }

    // // to show log only
    // React.useEffect(() => {
    //     console.log(orders);
    // }, [orders]);

    return { orders, handleOrderChange }
}