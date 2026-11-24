import axios from 'axios'
import config from './config'

export async function getWishlist(authorization) {
    const URL = config.BASE_URL + '/api/buyer/wishlist'
    const headers = { authorization }
    return await axios.get(URL, { headers })
}

export async function addToWishlist(carId, authorization) {
    const URL = config.BASE_URL + `/api/buyer/wishlist/${carId}`
    const headers = { authorization }
    return await axios.post(URL, {}, { headers })
}

export async function removeFromWishlist(carId, authorization) {
    const URL = config.BASE_URL + `/api/buyer/wishlist/${carId}`
    const headers = { authorization }
    return await axios.delete(URL, { headers })
}

export async function createBooking(carId, authorization) {
    const URL = config.BASE_URL + '/api/buyer/bookings'
    const headers = { authorization }
    const body = { carId }
    return await axios.post(URL, body, { headers })
}

export async function getMyBookings(authorization, page = 0, size = 10) {
    const URL = config.BASE_URL + '/api/buyer/bookings'
    const headers = { authorization }
    const params = { page, size }
    return await axios.get(URL, { headers, params })
}

export async function cancelBooking(bookingId, authorization) {
    const URL = config.BASE_URL + `/api/buyer/bookings/${bookingId}/cancel`
    const headers = { authorization }
    return await axios.patch(URL, {}, { headers })
}
