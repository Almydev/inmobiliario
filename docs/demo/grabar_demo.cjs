/* Graba el video de demostración (~90 s) de Inmobiliarias 360.
 * Requiere: front compilado sirviéndose en :4173, servidor_demo.py en :8080 y playwright-core.
 * Uso: node grabar_demo.cjs   → deja demo.webm en ./salida
 */
const { chromium } = require('playwright-core')
const fs = require('fs')
const path = require('path')

const BASE = 'http://localhost:4173'
const SALIDA = path.join(__dirname, 'salida')
fs.mkdirSync(SALIDA, { recursive: true })
for (const f of fs.readdirSync(SALIDA)) if (f.endsWith('.webm')) fs.unlinkSync(path.join(SALIDA, f))

const CHROME = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find((p) => fs.existsSync(p))
const pausa = (ms) => new Promise((r) => setTimeout(r, ms))

/** Elementos visuales que se superponen: cursor, rótulo inferior y tarjetas de inicio/cierre. */
const SUPERPUESTOS = () => {
  const css = document.createElement('style')
  css.textContent = `
    #demo-cursor{position:fixed;left:0;top:0;width:26px;height:26px;margin:-13px 0 0 -13px;border-radius:50%;
      background:rgba(201,169,138,.35);border:2px solid #c9a98a;box-shadow:0 2px 10px rgba(0,0,0,.35);
      pointer-events:none;z-index:2147483647;transition:transform .08s linear, background .15s}
    #demo-cursor.clic{background:rgba(201,169,138,.85);transform:scale(.8)}
    #demo-caption{position:fixed;left:50%;bottom:26px;transform:translate(-50%,12px);opacity:0;z-index:2147483646;
      max-width:min(900px,92vw);padding:12px 22px;border-radius:14px;background:rgba(15,31,38,.94);color:#f4ede4;
      font:500 17px/1.35 Inter,system-ui,sans-serif;box-shadow:0 10px 30px rgba(0,0,0,.35);
      transition:opacity .35s, transform .35s;text-align:center;pointer-events:none}
    #demo-caption.ver{opacity:1;transform:translate(-50%,0)}
    #demo-caption b{display:block;font:600 21px/1.2 Fraunces,Georgia,serif;color:#c9a98a;margin-bottom:3px}
    #demo-tarjeta{position:fixed;inset:0;z-index:2147483645;display:grid;place-items:center;text-align:center;
      background:radial-gradient(circle at 30% 20%,#25505f 0,#142a33 55%,#0e1d24 100%);color:#f4ede4;
      opacity:0;pointer-events:none;transition:opacity .6s}
    #demo-tarjeta.ver{opacity:1}
    #demo-tarjeta h1{font:600 54px/1.1 Fraunces,Georgia,serif;margin:0}
    #demo-tarjeta h1 span{color:#c9a98a}
    #demo-tarjeta p{margin:14px 0 0;font:400 22px Inter,system-ui,sans-serif;color:#dcc3a9;letter-spacing:.04em}`
  document.head.appendChild(css)
  const c = document.createElement('div'); c.id = 'demo-cursor'; document.body.appendChild(c)
  const cap = document.createElement('div'); cap.id = 'demo-caption'; document.body.appendChild(cap)
  const t = document.createElement('div'); t.id = 'demo-tarjeta'; document.body.appendChild(t)
  const mover = (e) => { c.style.transform = `translate(${e.clientX}px,${e.clientY}px)` }
  window.addEventListener('mousemove', mover, true)
  window.addEventListener('mousedown', () => c.classList.add('clic'), true)
  window.addEventListener('mouseup', () => c.classList.remove('clic'), true)
}

;(async () => {
  const browser = await chromium.launch({ executablePath: CHROME, headless: true })
  const ctx = await browser.newContext({
    viewport: { width: 1280, height: 720 },
    recordVideo: { dir: SALIDA, size: { width: 1280, height: 720 } },
    locale: 'es-CO',
  })
  await ctx.addInitScript(() => localStorage.removeItem('inmo360.token'))
  await ctx.addInitScript(() => localStorage.setItem('inmo360.tema', 'light'))
  await ctx.addInitScript(`document.addEventListener('DOMContentLoaded', (${SUPERPUESTOS.toString()}))`)
  const page = await ctx.newPage()
  await fetch('http://127.0.0.1:8080/api/reiniciar', { method: 'POST', body: '{}' })

  let x = 640
  let y = 360
  const rotulo = (titulo, texto) =>
    page.evaluate(([t, s]) => {
      const el = document.getElementById('demo-caption')
      el.innerHTML = `<b>${t}</b>${s}`
      el.classList.add('ver')
    }, [titulo, texto])
  const ocultarRotulo = () => page.evaluate(() => document.getElementById('demo-caption')?.classList.remove('ver'))
  const tarjeta = (html, visible) =>
    page.evaluate(([h, v]) => {
      const el = document.getElementById('demo-tarjeta')
      if (h) el.innerHTML = h
      el.classList.toggle('ver', v)
    }, [html, visible])

  const irA = async (px, py, pasos = 28) => {
    await page.mouse.move(px, py, { steps: pasos })
    x = px
    y = py
  }
  const centro = async (loc) => {
    await loc.scrollIntoViewIfNeeded()
    const b = await loc.boundingBox()
    return [b.x + b.width / 2, b.y + b.height / 2]
  }
  const clic = async (loc, espera = 350) => {
    const [cx, cy] = await centro(loc)
    await irA(cx, cy)
    await pausa(180)
    await loc.click()
    await pausa(espera)
  }
  const escribir = async (loc, texto, delay = 75) => {
    await clic(loc, 120)
    await loc.pressSequentially(texto, { delay })
  }
  const pasear = async (loc) => {
    const [cx, cy] = await centro(loc)
    await irA(cx, cy, 20)
  }
  const nav = (nombre) => page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: nombre, exact: true })

  /** Ejecuta una escena y completa el tiempo hasta su presupuesto para que el total sea ~90 s. */
  const escena = async (presupuesto, fn) => {
    const t0 = Date.now()
    await fn()
    const resto = presupuesto - (Date.now() - t0)
    if (resto > 0) await pausa(resto)
    else console.log(`  (la escena se pasó ${-resto} ms)`)
  }
  const inicio = Date.now()
  const marca = (n) => console.log(`${String((Date.now() - inicio) / 1000).padStart(5)} s  ${n}`)

  // ---------- 1. Introducción ----------
  await page.goto(BASE + '/login', { waitUntil: 'networkidle' })
  await tarjeta('<div><h1>Inmobiliarias <span>360</span></h1><p>Gestión de arriendos: cobros, pagos y cartera en un solo lugar</p></div>', true)
  await escena(3500, async () => {})
  marca('intro')
  await tarjeta(null, false)

  // ---------- 2. Acceso ----------
  await escena(8000, async () => {
    await rotulo('Acceso seguro', 'Ingreso con usuario y contraseña. Roles de administrador y operador.')
    await escribir(page.getByLabel('Correo electrónico'), 'admin@inmobiliaria360.co', 55)
    await escribir(page.getByLabel('Contraseña', { exact: true }), 'ClaveSegura-2026', 55)
    await clic(page.getByRole('button', { name: 'Ingresar' }), 200)
    await page.getByRole('heading', { name: /^Hola,/ }).waitFor()
  })
  marca('acceso')

  // ---------- 3. Inicio ----------
  await escena(10000, async () => {
    await rotulo('Panel principal', 'Lo importante del mes de un vistazo: recaudo, cartera vencida, banco y qué hacer ahora.')
    await pausa(2200)
    await irA(640, 420)
    for (let i = 0; i < 4; i++) {
      await page.mouse.wheel(0, 170)
      await pausa(700)
    }
    await pausa(1200)
    await page.mouse.wheel(0, -900)
    await pausa(900)
  })
  marca('inicio')

  // ---------- 4. Propietarios ----------
  await escena(7000, async () => {
    await rotulo('Propietarios', 'Los dueños de los inmuebles y sus datos de pago. Búsqueda en vivo y alta en un formulario.')
    await clic(nav('Propietarios'))
    await escribir(page.getByPlaceholder('Buscar por nombre'), 'Marta', 90)
    await pausa(900)
    await page.getByPlaceholder('Buscar por nombre').fill('')
    await clic(page.getByRole('button', { name: '+ Nuevo propietario' }))
    await pausa(1500)
    await clic(page.getByRole('button', { name: 'Cancelar' }), 100)
  })
  marca('propietarios')

  // ---------- 5. Inquilinos ----------
  await escena(5000, async () => {
    await rotulo('Inquilinos', 'Quienes reciben la cuenta de cobro, con su correo para el envío automático.')
    await clic(nav('Inquilinos'))
    await pasear(page.getByRole('row').nth(2))
    await pausa(500)
    await pasear(page.getByRole('row').nth(4))
  })
  marca('inquilinos')

  // ---------- 6. Inmuebles ----------
  await escena(6500, async () => {
    await rotulo('Inmuebles', 'Cada inmueble une propietario, inquilino y canon. Define el % de administración.')
    await clic(nav('Inmuebles'))
    await pasear(page.getByRole('row').nth(1))
    await pausa(800)
    await clic(page.getByRole('button', { name: '+ Nuevo inmueble' }))
    await pausa(1700)
    await clic(page.getByRole('button', { name: 'Cancelar' }), 100)
  })
  marca('inmuebles')

  // ---------- 7. Cuentas de cobro ----------
  await escena(13500, async () => {
    await rotulo('Cuentas de cobro', 'Genera todas las del mes con un clic, envíalas por correo con su PDF y marca los pagos.')
    await clic(nav('Cuentas de cobro'))
    await pausa(1200)
    await clic(page.getByRole('button', { name: 'Generar todas del mes' }))
    await pausa(1500)
    await clic(page.getByLabel('Seleccionar todos los pendientes'))
    await pausa(700)
    await clic(page.getByRole('button', { name: /^Enviar \d+ seleccionad/ }))
    await pausa(1800)
    await clic(page.getByRole('button', { name: 'Marcar pagada' }).first())
    await pausa(1200)
  })
  marca('cuentas')

  // ---------- 8. Cartera ----------
  await escena(6000, async () => {
    await rotulo('Cartera', 'Quién está pendiente de pago y hace cuántos días, con detalle por cuenta.')
    await clic(nav('Cartera'))
    await pausa(1500)
    await clic(page.getByRole('button', { name: /^Ver cuentas de/ }).first())
    await pausa(1200)
  })
  marca('cartera')

  // ---------- 9. Comprobantes de egreso ----------
  await escena(7000, async () => {
    await rotulo('Comprobantes de egreso', 'Pago a propietarios: canon menos administración, con PDF y envío por correo.')
    await clic(nav('Comprobantes de egreso'))
    await pausa(1000)
    await clic(page.getByRole('button', { name: 'Generar de cuentas pagadas' }))
    await pausa(1500)
    await clic(page.getByRole('button', { name: 'Enviar', exact: true }).first())
    await pausa(1500)
  })
  marca('egresos')

  // ---------- 10. Cuadre de banco ----------
  await escena(8500, async () => {
    await rotulo('Cuadre de banco', 'Ingresos, gastos y saldo del mes. Los pagos se registran solos; también puedes anotar movimientos.')
    await clic(nav('Cuadre de banco'))
    await pausa(1500)
    await clic(page.getByRole('button', { name: '+ Movimiento manual' }))
    await escribir(page.locator('#concepto'), 'comisión por transferencias', 28)
    await escribir(page.locator('#valor'), '12400', 60)
    await clic(page.getByRole('button', { name: 'Guardar' }))
    await pausa(800)
  })
  marca('banco')

  // ---------- 11. Usuarios ----------
  await escena(4000, async () => {
    await rotulo('Usuarios', 'El administrador crea accesos, restablece claves y desactiva usuarios al instante.')
    await clic(nav('Usuarios'))
    await pasear(page.getByRole('row').nth(2))
  })
  marca('usuarios')

  // ---------- 12. Tema y cuenta ----------
  await escena(5500, async () => {
    await rotulo('Tema claro y oscuro', 'Cómodo para cualquier hora, con la paleta del logo. Cuenta y cierre de sesión arriba a la derecha.')
    await clic(nav('Inicio'), 600)
    await clic(page.getByRole('button', { name: /Cambiar a tema oscuro/ }), 1200)
    await clic(page.getByRole('button', { name: /^Menú de / }), 1600)
  })
  marca('tema')

  // ---------- 13. Cierre ----------
  await page.keyboard.press('Escape')
  await ocultarRotulo()
  await tarjeta('<div><h1>Inmobiliarias <span>360</span></h1><p>Menos tiempo en papeles, más control de tu cartera</p></div>', true)
  await escena(3000, async () => {})
  marca('fin')

  const video = page.video()
  await ctx.close()
  const destino = path.join(SALIDA, 'demo.webm')
  await video.saveAs(destino)
  await browser.close()
  console.log('Video:', destino, `(${((Date.now() - inicio) / 1000).toFixed(1)} s de guion)`)
})().catch((e) => {
  console.error(e)
  process.exit(1)
})
