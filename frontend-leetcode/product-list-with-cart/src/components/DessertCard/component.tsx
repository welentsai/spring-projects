import * as React from 'react';
import Cart from '../../assets/images/icon-add-to-cart.svg'
import Plus from '../../assets/images/icon-increment-quantity.svg'
import Minus from '../../assets/images/icon-decrement-quantity.svg'

import { Dessert } from './type';
import { dessertReducer } from './hook';
import { useOrder } from '../../context/OrderContext';

export type DessertCardListProps = {
    desserts: Array<Dessert>
}

export const DesertCardList = ({ desserts }: DessertCardListProps) => {

    return (
        desserts.map((dessert: Dessert, index: number) => (
            <DessertCard
                dessert={dessert} key={index} />
        ))
    )
}

export type DessertCardProps = {
    dessert: Dessert
}

export const DessertCard = ({ dessert }: DessertCardProps) => {
    console.log('DessertCard Render!', Date.now(), dessert.name);

    const [dessertState, dispatchDessertAction] = React.useReducer(
        dessertReducer,
        { purchased: false, quantity: 0 });

    const { orders, handleOrderChange } = useOrder();

    const handleAddToCart = () => {
        dispatchDessertAction({ type: 'ADD_TO_CART' });
    }

    const handleDecrement = () => {
        dispatchDessertAction({ type: 'DECREMENT_ACTION', quantity: -1 });
        handleOrderChange({ dessert, quantity: -1 })
    }

    const handleIncrement = () => {
        dispatchDessertAction({ 'type': 'INCREMENT_ACTION', quantity: 1 });
        handleOrderChange({ dessert, quantity: 1 })
    }

    const memoIsOrdered = React.useMemo(() => orders.some((order) =>
        order.dessert.name === dessert.name)
        , [orders, dessert]);

    React.useEffect(() => {
        if (!memoIsOrdered) {
            dispatchDessertAction({ 'type': 'INIT_ACTION' })
        }
    }, [memoIsOrdered])

    return (
        <div>
            <div className='relative'>
                <img src={dessert.image.desktop} alt={dessert.name} />
                {dessertState.purchased === false ?
                    <AddToCart
                        handleAddToCart={handleAddToCart} />
                    : <Purchase
                        quantity={dessertState.quantity}
                        handleDecrement={handleDecrement}
                        handleIncrement={handleIncrement}
                    />
                }
            </div>
            <p className='text-left text-gray-400'>{dessert.category}</p>
            <p className='text-left font-bold'>{dessert.name}</p>
            <p className='text-left text-red-500'>${dessert.price}</p>
        </div>)
}


type AddToCartProps = {
    handleAddToCart: () => void;
}

const AddToCart = ({ handleAddToCart }: AddToCartProps) =>
(
    <div className='absolute left-1/2 bottom-0 transform -translate-x-1/2 translate-y-1/2'>
        <button className='px-4 py-1 flex items-center justify-center text-sm bg-white rounded-full border border-red-300'
            onClick={() => handleAddToCart()}>
            <img src={Cart} className='p-1' />
            <span>Add to Cart</span>
        </button>
    </div>
)

type PurchaseProps = {
    quantity: number;
    handleIncrement: () => void;
    handleDecrement: () => void;
}

const Purchase = ({ quantity, handleIncrement, handleDecrement }: PurchaseProps) =>
(
    <div className='absolute left-1/2 bottom-0 transform -translate-x-1/2 translate-y-1/2'>
        <div className='px-6 py-1 flex items-center justify-center bg-orange-600 rounded-full overflow-hidden '>
            <button
                className='w-6 h-6 -ml-3 rounded-full border border-orange-50 text-white flex items-center justify-center'
                onClick={() => handleDecrement()}>
                <img src={Minus} />
            </button>
            <span className='px-8 py-1 text-orange-100 font-semibold'>{quantity}</span>
            <button
                className='w-6 h-6 -mr-3 rounded-full border border-orange-50 text-white flex items-center justify-center'
                onClick={() => handleIncrement()}>
                <img src={Plus} />
            </button>
        </div>
    </div>
)