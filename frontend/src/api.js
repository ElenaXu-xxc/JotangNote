const API_PREFIX = '/api'

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function postJson(path, body) {
  const { rawBody } = await request(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  })

  return rawBody.trim()
}

export async function postJsonObject(path, body) {
  const rawBody = await postJson(path, body)

  if (!rawBody) {
    throw new ApiError('后端返回了空响应', 200)
  }

  try {
    return JSON.parse(rawBody)
  } catch {
    throw new ApiError('后端返回了无法解析的数据', 200)
  }
}

export async function putJson(path, body) {
  const { rawBody } = await request(path, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  })

  return rawBody.trim()
}

export async function deleteJson(path) {
  const { rawBody } = await request(path, {
    method: 'DELETE',
  })

  return rawBody.trim()
}

export async function getJson(path) {
  const { rawBody } = await request(path, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!rawBody) {
    throw new ApiError('后端返回了空响应', 200)
  }

  try {
    return JSON.parse(rawBody)
  } catch {
    throw new ApiError('后端返回了无法解析的数据', 200)
  }
}

async function request(path, options) {
  let response

  try {
    response = await fetch(`${API_PREFIX}${path}`, {
      credentials: 'include',
      ...options,
    })
  } catch {
    throw new ApiError(
      '无法连接后端，请确认 Spring Boot 已在 8080 端口启动',
      0,
    )
  }

  const rawBody = await response.text()

  if (!response.ok) {
    throw new ApiError(
      extractErrorMessage(rawBody) || `请求失败（HTTP ${response.status}）`,
      response.status,
    )
  }

  return { rawBody }
}

function extractErrorMessage(rawBody) {
  if (!rawBody) {
    return ''
  }

  try {
    const data = JSON.parse(rawBody)
    return data.message || data.error || rawBody
  } catch {
    return rawBody.length > 300 ? rawBody.slice(0, 300) : rawBody
  }
}
