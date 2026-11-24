import axios from 'axios'
import config from './config'

export async function getSellerCars(authorization, page = 0, size = 10) {
    const URL = config.BASE_URL + '/api/seller/cars'
    const headers = { authorization }
    const params = { page, size }
    return await axios.get(URL, { headers, params })
}

export async function addCar(carData, imagesData, authorization) {
    const URL = config.BASE_URL + '/api/seller/cars'
    const formData = new FormData()
    formData.append('car', new Blob([JSON.stringify(carData)], { type: 'application/json' }))

    // Handle 3-section image upload
    if (imagesData && typeof imagesData === 'object' && !Array.isArray(imagesData)) {
        if (imagesData.primaryImage) {
            formData.append('primaryImage', imagesData.primaryImage)
        }
        if (imagesData.exteriorImages && imagesData.exteriorImages.length > 0) {
            Array.from(imagesData.exteriorImages).forEach(file => {
                formData.append('exteriorImages', file)
            })
        }
        if (imagesData.interiorImages && imagesData.interiorImages.length > 0) {
            Array.from(imagesData.interiorImages).forEach(file => {
                formData.append('interiorImages', file)
            })
        }
    } else if (imagesData && imagesData.length > 0) {
        // Fallback for simple flat image list
        Array.from(imagesData).forEach(file => {
            formData.append('images', file)
        })
    }

    const headers = {
        authorization,
        'Content-Type': 'multipart/form-data'
    }
    return await axios.post(URL, formData, { headers })
}

export async function deleteCar(id, authorization) {
    const URL = config.BASE_URL + `/api/seller/cars/${id}`
    const headers = { authorization }
    return await axios.delete(URL, { headers })
}

export async function getSellerKyc(authorization) {
    const URL = config.BASE_URL + '/api/seller/kyc'
    const headers = { authorization }
    return await axios.get(URL, { headers })
}

export async function submitSellerKyc(documentType, documentNumber, file, authorization) {
    const URL = config.BASE_URL + '/api/seller/kyc'
    const formData = new FormData()
    formData.append('documentType', documentType)
    formData.append('documentNumber', documentNumber)
    formData.append('file', file)
    const headers = {
        authorization,
        'Content-Type': 'multipart/form-data'
    }
    return await axios.post(URL, formData, { headers })
}

export async function getSellerBookings(authorization, page = 0, size = 10) {
    const URL = config.BASE_URL + '/api/seller/bookings'
    const headers = { authorization }
    const params = { page, size }
    return await axios.get(URL, { headers, params })
}

export async function completeBooking(id, authorization) {
    const URL = config.BASE_URL + `/api/seller/bookings/${id}/complete`
    const headers = { authorization }
    return await axios.patch(URL, {}, { headers })
}
