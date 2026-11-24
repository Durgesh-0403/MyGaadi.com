import { createSlice } from '@reduxjs/toolkit'

const WishlistSlice = createSlice({
    name: 'wishlist',
    initialState: {
        items: [],
        count: 0
    },
    reducers: {
        setWishlistAction: (state, { payload }) => {
            state.items = payload || []
            state.count = state.items.length
        },
        addToWishlistAction: (state, { payload }) => {
            const exists = state.items.some(car => car.id === payload.id)
            if (!exists) {
                state.items.push(payload)
                state.count = state.items.length
            }
        },
        removeFromWishlistAction: (state, { payload }) => {
            state.items = state.items.filter(car => car.id !== payload)
            state.count = state.items.length
        },
        clearWishlistAction: (state) => {
            state.items = []
            state.count = 0
        }
    }
})

export default WishlistSlice.reducer
export const {
    setWishlistAction,
    addToWishlistAction,
    removeFromWishlistAction,
    clearWishlistAction
} = WishlistSlice.actions
