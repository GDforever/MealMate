export function isEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

export function isUsernameValid(username: string): boolean {
  return username.length >= 3 && username.length <= 20
}

export function isPasswordValid(password: string): boolean {
  return password.length >= 6
}

export function isLatitude(lat: number): boolean {
  return lat >= -90 && lat <= 90
}

export function isLongitude(lng: number): boolean {
  return lng >= -180 && lng <= 180
}
