import { useRef } from 'react'

/**
 * Evita que una accion se ejecute dos veces por multiples clics: mientras una esta en curso, las demas se ignoran.
 * A diferencia de deshabilitar el boton con useState (que tarda un render), el bloqueo es inmediato.
 */
export function useBloqueo() {
  const enCurso = useRef(false)
  return async (fn) => {
    if (enCurso.current) return undefined
    enCurso.current = true
    try {
      return await fn()
    } finally {
      enCurso.current = false
    }
  }
}
