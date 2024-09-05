type DessertCardState = {
    purchased: boolean,
    quantity: number
}

type InitAction = {
    type: 'INIT_ACTION'
}

type AddToCartAction = {
    type: 'ADD_TO_CART'
}

type IncrementAction = {
    type: 'INCREMENT_ACTION',
    quantity: number
}

type DecrementAction = {
    type: 'DECREMENT_ACTION',
    quantity: number
}

type DessertCardAction = InitAction | AddToCartAction | IncrementAction | DecrementAction;


export const dessertReducer = (state: DessertCardState, action: DessertCardAction): DessertCardState => {
    switch (action.type) {
        case 'INIT_ACTION':
            return {
                ...state,
                purchased: false,
                quantity: 0
            }
        case 'ADD_TO_CART':
            return {
                ...state,
                purchased: true
            }
        case 'INCREMENT_ACTION':
            return {
                ...state,
                purchased: true,
                quantity: state.quantity + action.quantity
            }
        case 'DECREMENT_ACTION':
            return state.quantity <= 0 ? {
                ...state,
                purchased: false,
            } : {
                ...state,
                quantity: state.quantity + action.quantity
            }

    }
}