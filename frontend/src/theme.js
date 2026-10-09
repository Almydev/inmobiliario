import { useCallback, useEffect, useState } from 'react'

const KEY = 'inmo360.tema'

function guardado() {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

/** Tema inicial: el elegido por el usuario o, si no hay, el del sistema operativo. */
export function temaInicial() {
  const t = guardado()
  if (t === 'dark' || t === 'light') return t
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function aplicarTema(t) {
  document.documentElement.dataset.theme = t
}

export function useTema() {
  const [tema, setTema] = useState(() => document.documentElement.dataset.theme || temaInicial())

  useEffect(() => {
    aplicarTema(tema)
  }, [tema])

  const alternar = useCallback(() => {
    setTema((actual) => {
      const nuevo = actual === 'dark' ? 'light' : 'dark'
      try {
        localStorage.setItem(KEY, nuevo)
      } catch {
        /* navegacion privada: el tema vale solo para esta sesion */
      }
      return nuevo
    })
  }, [])

  return [tema, alternar]
}
