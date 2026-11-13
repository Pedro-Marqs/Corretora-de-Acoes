let accessToken = null
let sessionGeneration = 0

export function getAdminAccessToken() { return accessToken }
export function getAdminSessionGeneration() { return sessionGeneration }

export function beginAdminSession() {
  sessionGeneration += 1
  accessToken = null
  return sessionGeneration
}

export function setAdminAccessTokenForGeneration(token, generation) {
  if (generation !== sessionGeneration) return false
  accessToken = token
  return true
}

export function endAdminSession() {
  sessionGeneration += 1
  accessToken = null
  return sessionGeneration
}

export function clearAdminAccessToken(expectedToken, expectedGeneration) {
  const tokenMatches = arguments.length === 0 || accessToken === expectedToken
  const generationMatches = expectedGeneration === undefined || sessionGeneration === expectedGeneration
  if (tokenMatches && generationMatches) accessToken = null
}
