import * as React from 'react';
import { v4 as uuidv4 } from 'uuid';
import EmptyCart from '../../assets/images/illustration-empty-cart.svg'
import OrderConfirmed from '../../assets/images/icon-order-confirmed.svg'
import { Order, useOrder } from '../../context/OrderContext';

export const ShoppingCard = () => {
    const [openModel, setOpenModel] = React.useState<boolean>(false);

    const { orders, handleOrderChange } = useOrder();

    const orderCount = orders.reduce((total, order) => total + order.quantity, 0)

    function handleRemoveOrder(target: Order) {
        handleOrderChange({ ...target, quantity: 0 - target.quantity });
    }

    function openConfirmModel(): void {
        setOpenModel(true);
    }

    function closeModel(): void {
        setOpenModel(false);
    }

    React.useEffect(() => {
        setOpenModel(false);
    }, [])

    return (
        <div className='p-4'>
            <h2 className='text-lg text-left font-bold mb-3 text-orange-600'>Your Cart({orderCount})</h2>
            {orders.length === 0 ? (
                <div className='text-center'>
                    <img src={EmptyCart} className='mx-auto w-1/3' />
                    <p className='text-gray-500 text-xs'>Your added items will appear here</p>
                </div>) : (
                <div>
                    {orders.map((order) => (
                        <div key={uuidv4()}>
                            <div className='flex items-center justify-between'>
                                <div>
                                    <p className='text-left text-xs font-bold'>{order.dessert.name}</p>
                                    <div className='flex'>
                                        <p className='px-1 font-bold text-left text-sm text-orange-500'>{order.quantity}<span className='text-xs'>x</span></p>
                                        <p className='px-1 text-left text-sm text-orange-300'><span className='text-xs'>@</span>{order.dessert.price}</p>
                                        <p className='px-1 text-left text-sm text-orange-500'>${order.quantity * order.dessert.price}</p>
                                    </div>
                                </div>
                                <button
                                    className='w-4 h-4 rounded-full border border-orange-300 text-xs text-orange-500 flex items-center justify-center'
                                    onClick={() => handleRemoveOrder(order)}>
                                    X
                                </button>

                            </div>
                            <div className='w-full h-px bg-gray-300 my-1' />
                        </div>
                    ))}
                    <div className='flex items-center justify-between'>
                        <div className='text-sm text-gray-500'> Order Total </div>
                        <div className='text-base font-bold'>
                            ${orders
                                .reduce((total, order) => total + order.quantity * order.dessert.price, 0)} </div>
                    </div>
                    <button
                        className='w-full py-1.5 mt-5 flex items-center justify-center  bg-orange-600 rounded-full text-white text-xs'
                        onClick={() => openConfirmModel()}
                    >
                        Confirm Order
                    </button>

                    {openModel &&
                        <ConfirmedModal orders={orders} handleCloseModal={closeModel} />
                    }
                </div>
            )}

        </div>
    )
}

type ConfirmedModelProps = {
    orders: Array<Order>,
    handleCloseModal: () => void
}

const ConfirmedModal = ({ orders, handleCloseModal }: ConfirmedModelProps) => {
    const totalPrice = orders
        .reduce((total, order) => total + order.quantity * order.dessert.price, 0);

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center p-4">
            <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
                <div className='flex'>
                    <img src={OrderConfirmed} className='mr-auto w-8' />
                </div>
                <p className='font-bold text-left text-2xl'>Order Confirmed</p>
                <p className='m-2 text-left text-xs text-gray-300'> we hope you enjoy your food !!</p>
                <div >
                    {orders.map((order) => (
                        <OrderItem key={uuidv4()} order={order} />
                    ))}
                    <div className='flex items-center justify-between mx-5 bg-gray-200'>
                        <div className='m-2 text-xs text-gray-500'> Order Total </div>
                        <div className='m-2 text-sm font-bold'>${totalPrice} </div>
                    </div>
                    <div className='mx-5'>
                        <button
                            className='w-full py-1.5 mt-5 mb-3 flex items-center justify-center  bg-orange-600 rounded-full text-white text-xs'
                            onClick={() => handleCloseModal()}
                        >
                            Start New Order
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}

type OrderItemProps = {
    order: Order
}

const OrderItem = ({ order }: OrderItemProps) => (
    <div className="flex justify-between items-center mx-5 bg-gray-200">
        <div className="flex m-2">
            <img className="w-10 h-10" src={order.dessert.image.desktop} alt={order.dessert.name} />
            <div className="mx-2">
                <p className="text-left text-xs">{order.dessert.name}</p>
                <div className="flex">
                    <p className="my-1 text-left text-xs text-orange-500">{order.quantity}<span className="text-xs">x</span></p>
                    <p className="my-1 text-left text-xs text-gray-400"><span className="text-xs">@</span>{order.dessert.price}</p>
                </div>
            </div>
        </div>
        <p className="text-xs px-2">${order.quantity * order.dessert.price}</p>
    </div>
);