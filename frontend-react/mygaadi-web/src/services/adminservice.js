import axios from 'axios'
import config from './config'

export async function getDashboardStats(authorization) {
    const URL = config.BASE_URL + '/api/admin/dashboard'
    const headers = { authorization }
    return await axios.get(URL, { headers })
}

export async function getAdminCars(authorization, approvalStatus = null, deleted = null, q = null, page = 0, size = 15) {
    const URL = config.BASE_URL + '/api/admin/cars'
    const headers = { authorization }
    const params = { page, size }
    if (approvalStatus) params.approvalStatus = approvalStatus
    if (deleted !== null) params.deleted = deleted
    if (q) params.q = q
    return await axios.get(URL, { headers, params })
}

export async function approveCar(id, authorization) {
    const URL = config.BASE_URL + `/api/admin/cars/${id}/approve`
    const headers = { authorization }
    return await axios.patch(URL, {}, { headers })
}

export async function rejectCar(id, reason, authorization) {
    const URL = config.BASE_URL + `/api/admin/cars/${id}/reject`
    const headers = { authorization }
    const body = { reason }
    return await axios.patch(URL, body, { headers })
}

export async function getAdminUsers(authorization, role = null, q = null, page = 0, size = 15) {
    const URL = config.BASE_URL + '/api/admin/users'
    const headers = { authorization }
    const params = { page, size }
    if (role) params.role = role
    if (q) params.q = q
    return await axios.get(URL, { headers, params })
}

export async function updateSellerKycStatus(sellerId, status, rejectionReason, authorization) {
    const URL = config.BASE_URL + `/api/admin/sellers/${sellerId}/kyc`
    const headers = { authorization }
    const body = { status, rejectionReason }
    return await axios.patch(URL, body, { headers })
}

export async function getAdminBookings(authorization, page = 0, size = 15) {
    const URL = config.BASE_URL + '/api/admin/bookings'
    const headers = { authorization }
    const params = { page, size }
    return await axios.get(URL, { headers, params })
}

export async function blockUserAdmin(id, authorization) {
    const URL = config.BASE_URL + `/api/admin/users/${id}/block`
    const headers = { authorization }
    return await axios.patch(URL, {}, { headers })
}

export async function unblockUserAdmin(id, authorization) {
    const URL = config.BASE_URL + `/api/admin/users/${id}/unblock`
    const headers = { authorization }
    return await axios.patch(URL, {}, { headers })
}

export async function deleteUserAdmin(id, authorization) {
    const URL = config.BASE_URL + `/api/admin/users/${id}`
    const headers = { authorization }
    return await axios.delete(URL, { headers })
}
