import axios from 'axios'
import config from './config'

export async function getCars(params = {}) {
    const URL = config.BASE_URL + '/api/cars'
    return await axios.get(URL, { params })
}

export async function getCarById(id) {
    const URL = config.BASE_URL + `/api/cars/${id}`
    return await axios.get(URL)
}

export function getImageUrl(imagePath) {
    if (!imagePath) {
        return 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80'
    }
    if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
        return imagePath
    }
    return `${config.BASE_URL}${imagePath.startsWith('/') ? '' : '/'}${imagePath}`
}

export function formatPrice(price) {
    if (!price && price !== 0) return '₹ --'
    const num = Number(price)
    if (num >= 10000000) {
        return `₹ ${(num / 10000000).toFixed(2)} Cr`
    } else if (num >= 100000) {
        return `₹ ${(num / 100000).toFixed(2)} Lakh`
    }
    return `₹ ${num.toLocaleString('en-IN')}`
}
