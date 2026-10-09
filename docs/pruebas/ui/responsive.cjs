const { chromium } = require('playwright-core')
const fs = require('fs')
const path = require('path')

const CAPTURAS = path.join(__dirname, 'capturas')
fs.mkdirSync(CAPTURAS, { recursive: true })
const URL = 'http://localhost:4173'
const CHROME = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find((p) => fs.existsSync(p))

const VISTAS = [
  { nombre: 'movil', w: 390, h: 844 },
  { nombre: 'movil-pequeno', w: 320, h: 640 },
  { nombre: 'tablet', w: 768, h: 1024 },
  { nombre: 'portatil', w: 1024, h: 768 },
  { nombre: 'escritorio', w: 1440, h: 900 },
]
const RUTAS = ['/', '/cartera', '/cuentas-cobro', '/comprobantes-egreso', '/banco', '/propietarios', '/inmuebles', '/usuarios', '/cuenta/password']

const resultados = []
const ok = (c, msg) => resultados.push({ c, msg })

;(async () => {
  const browser = await chromium.launch({ executablePath: CHROME, headless: true })
  for (const v of VISTAS) {
    const ctx = await browser.newContext({ viewport: { width: v.w, height: v.h }, deviceScaleFactor: 1 })
    await ctx.addInitScript(() => localStorage.setItem('inmo360.token', 'x'))
    const page = await ctx.newPage()
    const errores = []
    page.on('pageerror', (e) => errores.push(e.message))

    // 1. Sin desbordamiento horizontal en ninguna pantalla
    for (const ruta of RUTAS) {
      await page.goto(URL + ruta, { waitUntil: 'networkidle' })
      await page.waitForTimeout(250)
      const m = await page.evaluate(() => ({ sw: document.documentElement.scrollWidth, iw: window.innerWidth }))
      ok(m.sw <= m.iw, `[${v.nombre} ${v.w}px] ${ruta}: sin scroll horizontal (ancho ${m.sw} / ventana ${m.iw})`)
    }

    // 2. Barra superior y menú de usuario
    await page.goto(URL + '/', { waitUntil: 'networkidle' })
    const cabecera = page.locator('header').first()
    const caja = await cabecera.boundingBox()
    ok(caja && caja.y === 0 && caja.width >= v.w - (v.w >= 1024 ? 256 : 0) - 1, `[${v.nombre}] barra superior arriba y a todo el ancho`)
    await page.screenshot({ path: path.join(CAPTURAS, `r-${v.nombre}-inicio.png`) })

    const boton = page.getByRole('button', { name: /^Menú de / })
    await boton.click()
    const menu = page.getByRole('menu')
    await menu.waitFor()
    await page.waitForTimeout(350)
    const mb = await menu.boundingBox()
    ok(mb && mb.x >= 0 && mb.x + mb.width <= v.w && mb.y + mb.height <= v.h, `[${v.nombre}] el menú de usuario cabe en pantalla (${Math.round(mb.x)}..${Math.round(mb.x + mb.width)} de ${v.w})`)
    ok(await page.getByRole('menuitem', { name: 'Cambiar contraseña' }).isVisible(), `[${v.nombre}] "Cambiar contraseña" visible`)
    ok(await page.getByRole('menuitem', { name: 'Cerrar sesión' }).isVisible(), `[${v.nombre}] "Cerrar sesión" visible`)
    await page.screenshot({ path: path.join(CAPTURAS, `r-${v.nombre}-menu.png`) })
    await page.keyboard.press('Escape')
    ok((await page.getByRole('menu').count()) === 0, `[${v.nombre}] Esc cierra el menú de usuario`)

    // 3. Tema
    const antes = await page.evaluate(() => document.documentElement.dataset.theme)
    await page.getByRole('button', { name: /Cambiar a tema/ }).click()
    const despues = await page.evaluate(() => document.documentElement.dataset.theme)
    ok(antes !== despues, `[${v.nombre}] el botón cambia el tema (${antes} → ${despues})`)
    await page.waitForTimeout(500)
    await page.screenshot({ path: path.join(CAPTURAS, `r-${v.nombre}-tema.png`) })
    await page.getByRole('button', { name: /Cambiar a tema/ }).click()

    // 4. Navegación: lateral fija en escritorio, menú desplegable en el resto
    if (v.w >= 1024) {
      ok(await page.getByRole('navigation', { name: 'Principal' }).isVisible(), `[${v.nombre}] barra lateral visible`)
      ok((await page.getByRole('button', { name: 'Abrir menú' }).isVisible()) === false, `[${v.nombre}] sin botón de hamburguesa`)
    } else {
      ok((await page.getByRole('navigation', { name: 'Principal' }).count()) === 0, `[${v.nombre}] barra lateral oculta`)
      await page.getByRole('button', { name: 'Abrir menú' }).click()
      const nav = page.getByRole('navigation', { name: 'Principal' })
      await nav.waitFor()
      await page.waitForTimeout(350)
      const nb = await page.getByRole('dialog').locator('div').nth(1).boundingBox()
      ok(nb && nb.x >= 0 && nb.x + nb.width <= v.w, `[${v.nombre}] el menú lateral cabe en pantalla`)
      await page.screenshot({ path: path.join(CAPTURAS, `r-${v.nombre}-drawer.png`) })
      await nav.getByRole('link', { name: 'Cartera' }).click()
      await page.waitForURL('**/cartera')
      ok((await page.getByRole('dialog').count()) === 0, `[${v.nombre}] el menú se cierra al navegar`)
      // Esc también cierra
      await page.getByRole('button', { name: 'Abrir menú' }).click()
      await page.keyboard.press('Escape')
      ok((await page.getByRole('dialog').count()) === 0, `[${v.nombre}] Esc cierra el menú de navegación`)
    }

    // 5. Cambiar contraseña y cerrar sesión desde el menú
    await page.getByRole('button', { name: /^Menú de / }).click()
    await page.getByRole('menuitem', { name: 'Cambiar contraseña' }).click()
    await page.waitForURL('**/cuenta/password')
    await page.getByRole('heading', { name: 'Cambiar contraseña' }).waitFor()
    ok(await page.getByRole('heading', { name: 'Cambiar contraseña' }).isVisible(), `[${v.nombre}] abre la pantalla de cambio de contraseña`)
    await page.screenshot({ path: path.join(CAPTURAS, `r-${v.nombre}-password.png`) })
    await page.getByRole('button', { name: /^Menú de / }).click()
    await page.getByRole('menuitem', { name: 'Cerrar sesión' }).click()
    await page.waitForURL('**/login')
    ok(true, `[${v.nombre}] cerrar sesión lleva al login`)
    ok(errores.length === 0, `[${v.nombre}] sin errores de JavaScript ${errores[0] || ''}`)
    await ctx.close()
  }
  await browser.close()

  const fallos = resultados.filter((r) => !r.c)
  for (const r of resultados) console.log((r.c ? 'OK    ' : 'FALLA ') + r.msg)
  console.log(`\n${resultados.length - fallos.length}/${resultados.length} comprobaciones correctas`)
  process.exit(fallos.length ? 1 : 0)
})().catch((e) => {
  console.error(e)
  process.exit(2)
})
