let accessToken = null
let sessionGeneration = 0

export function getAccessToken() {
  return accessToken
}

export function getSessionGeneration() {
  return sessionGeneration
}

export function beginSession() {
  return startSession(null)
}

export function startSession(token) {
  sessionGeneration += 1
  accessToken = token
  return sessionGeneration
}

export function setAccessTokenForGeneration(token, expectedGeneration) {
  if (sessionGeneration !== expectedGeneration) return false
  accessToken = token
  return true
}

export function endSession() {
  sessionGeneration += 1
  accessToken = null
  return sessionGeneration
}

export function clearAccessToken(expectedToken, expectedGeneration) {
  const tokenMatches = arguments.length === 0 || accessToken === expectedToken
  const generationMatches = expectedGeneration === undefined || sessionGeneration === expectedGeneration
  if (tokenMatches && generationMatches) accessToken = null
}

