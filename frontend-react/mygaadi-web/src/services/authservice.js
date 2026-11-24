import axios from 'axios'
import config from './config'

export async function userSignin(email, password) {
    const URL = config.BASE_URL + '/api/auth/login'
    const body = { email, password }
    return await axios.post(URL, body)
}

export async function adminSignin(email, password) {
    const URL = config.BASE_URL + '/api/auth/admin/login'
    const body = { email, password }
    return await axios.post(URL, body)
}

export async function buyerSignup(body) {
    const URL = config.BASE_URL + '/api/auth/register/buyer'
    return await axios.post(URL, body)
}

export async function sellerSignup(body) {
    const URL = config.BASE_URL + '/api/auth/register/seller'
    return await axios.post(URL, body)
}

export function getStoredToken() {
    return window.sessionStorage.getItem('token')
}

export function getStoredUser() {
    const userStr = window.sessionStorage.getItem('user')
    if (userStr) {
        try {
            return JSON.parse(userStr)
        } catch (e) {
            return null
        }
    }
    return null
}

export function setSessionAuth(token, user) {
    window.sessionStorage.setItem('token', token.startsWith('Bearer ') ? token : 'Bearer ' + token)
    window.sessionStorage.setItem('user', JSON.stringify(user))
}

export function clearSessionAuth() {
    window.sessionStorage.removeItem('token')
    window.sessionStorage.removeItem('user')
}
