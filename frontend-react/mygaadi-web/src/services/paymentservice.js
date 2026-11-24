import axios from 'axios'
import config from './config'

export async function createPaymentOrder(bookingId, authorization) {
    const URL = config.BASE_URL + '/api/payments/orders'
    const headers = { authorization }
    const body = { bookingId }
    return await axios.post(URL, body, { headers })
}

export async function verifyPayment(verificationData, authorization) {
    const URL = config.BASE_URL + '/api/payments/verify'
    const headers = { authorization }
    return await axios.post(URL, verificationData, { headers })
}

export async function recordPaymentFailure(failureData, authorization) {
    const URL = config.BASE_URL + '/api/payments/failure'
    const headers = { authorization }
    return await axios.post(URL, failureData, { headers })
}
