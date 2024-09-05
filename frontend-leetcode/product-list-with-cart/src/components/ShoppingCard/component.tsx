import EmptyCart from '../../assets/images/illustration-empty-cart.svg'
import { Order, useOrder } from '../../context/OrderContext';

export const ShoppingCard = () => {
    const { orders, handleOrderChange } = useOrder();

    const orderCount = orders.reduce((total, order) => total + order.quantity, 0)

    function handleRemoveOrder(target: Order) {
        handleOrderChange({ ...target, quantity: 0 - target.quantity });
    }

    return (
        <div className='p-4'>
            <h2 className='text-lg text-left font-bold mb-3 text-orange-600'>Your Cart({orderCount})</h2>
            {orders.length === 0 ? (
                <div className='text-center'>
                    <img src={EmptyCart} className='mx-auto w-1/3' />
                    <p className='text-gray-500 text-xs'>Your added items will appear here</p>
                </div>) : (
                <div>
                    {orders.map((order, index) => (
                        <div>
                            <div key={order.dessert.name + index} className='flex items-center justify-between'>
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
                    <button className='w-full py-1.5 mt-5 flex items-center justify-center  bg-orange-600 rounded-full text-white text-xs'>
                        Confirm Order
                    </button>
                </div>
            )}

        </div>
    )
}