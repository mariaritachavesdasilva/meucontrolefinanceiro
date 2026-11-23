package com.meucontrolefinanceiro.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import android.content.Context
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var db: FinanceDatabase
    private val prefs by lazy { getSharedPreferences("app_preferences", Context.MODE_PRIVATE) }
    private var page = "Início"
    private val pink = Color.rgb(201, 111, 145)
    private val lightPink = Color.rgb(248, 232, 237)
    private val beige = Color.rgb(247, 241, 232)
    private val ink = Color.rgb(55, 48, 51)
    private val muted = Color.rgb(133, 122, 127)
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val categories = arrayOf("Alimentação", "Moradia", "Transporte", "Saúde", "Lazer", "Salário", "Freelance", "Compras", "Educação", "Outros")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FinanceDatabase(this)
        render()
    }

    private fun premium() = prefs.getBoolean("premium", false)
    private fun monthKey() = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private fun entriesThisMonth() = db.all().filter { it.date.startsWith(monthKey()) }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(255, 249, 246)) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 22, 22, 12) }
        header.addView(label("Meu Controle Financeiro", 23, ink, true))
        header.addView(label(if (premium()) "Conta Premium" else "Organize seu dinheiro com leveza", 13, muted, false).apply { setPadding(0, 4, 0, 0) })
        root.addView(header)
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 8, 18, 18) }
        when (page) {
            "Início" -> dashboard(body)
            "Receitas" -> entryList(body, "RECEITA")
            "Despesas" -> entryList(body, "DESPESA")
            "Relatórios" -> reports(body)
            "Planos" -> plans(body)
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); setPadding(5, 8, 5, 8) }
        listOf("Início", "Receitas", "Despesas", "Relatórios", "Planos").forEach { item ->
            val b = TextView(this).apply {
                text = item; textSize = 11f; gravity = Gravity.CENTER; setTextColor(if (page == item) pink else muted)
                setPadding(2, 8, 2, 8); typeface = if (page == item) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setOnClickListener { page = item; render() }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun dashboard(body: LinearLayout) {
        val current = entriesThisMonth()
        val income = current.filter { it.type == "RECEITA" }.sumOf { it.amount }
        val expense = current.filter { it.type == "DESPESA" }.sumOf { it.amount }
        val balance = db.all().filter { it.date <= today() }.sumOf { if (it.type == "RECEITA") it.amount else -it.amount }
        body.addView(card {
            addView(label("SALDO ATUAL", 12, muted, true))
            addView(label(currency.format(balance), 30, ink, true).apply { setPadding(0, 6, 0, 0) })
            addView(label("Resumo de ${SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")).format(Date())}", 13, muted, false).apply { setPadding(0, 5, 0, 0) })
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(card { addView(label("RECEITAS", 11, muted, true)); addView(label(currency.format(income), 18, Color.rgb(65, 142, 111), true).apply { setPadding(0, 6, 0, 0) }) }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 6 })
        row.addView(card { addView(label("DESPESAS", 11, muted, true)); addView(label(currency.format(expense), 18, pink, true).apply { setPadding(0, 6, 0, 0) }) }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = 6 })
        body.addView(row)
        body.addView(label("Últimos lançamentos", 18, ink, true).apply { setPadding(4, 20, 0, 10) })
        val recent = db.all().take(5)
        if (recent.isEmpty()) body.addView(card { addView(label("Seus lançamentos aparecerão aqui.", 14, muted, false)) })
        recent.forEach { body.addView(entryRow(it, false)) }
        body.addView(button("+ Nova receita", pink) { showEntryDialog("RECEITA") })
        body.addView(button("+ Nova despesa", Color.rgb(160, 130, 139)) { showEntryDialog("DESPESA") })
    }

    private fun entryList(body: LinearLayout, type: String) {
        val title = if (type == "RECEITA") "Receitas" else "Despesas"
        body.addView(label(title, 22, ink, true).apply { setPadding(4, 4, 0, 12) })
        body.addView(button("+ Adicionar ${if (type == "RECEITA") "receita" else "despesa"}", pink) { showEntryDialog(type) })
        val list = db.all().filter { it.type == type }
        if (list.isEmpty()) body.addView(card { addView(label("Nenhum lançamento cadastrado ainda.", 14, muted, false)) })
        list.forEach { body.addView(entryRow(it, type == "DESPESA")) }
    }

    private fun reports(body: LinearLayout) {
        val list = if (premium()) db.all() else entriesThisMonth()
        val income = list.filter { it.type == "RECEITA" }.sumOf { it.amount }
        val expense = list.filter { it.type == "DESPESA" }.sumOf { it.amount }
        body.addView(label(if (premium()) "Relatório financeiro completo" else "Relatório mensal", 22, ink, true).apply { setPadding(4, 4, 0, 12) })
        body.addView(card {
            addView(label("Receitas", 13, muted, false)); addView(label(currency.format(income), 20, Color.rgb(65, 142, 111), true))
            addView(label("Despesas", 13, muted, false).apply { setPadding(0, 10, 0, 0) }); addView(label(currency.format(expense), 20, pink, true))
            addView(label("Saldo do mês", 13, muted, false).apply { setPadding(0, 10, 0, 0) }); addView(label(currency.format(income - expense), 23, ink, true))
        })
        body.addView(label("Gastos por categoria", 18, ink, true).apply { setPadding(4, 18, 0, 10) })
        val grouped = list.filter { it.type == "DESPESA" }.groupBy { it.category }.mapValues { it.value.sumOf(FinanceEntry::amount) }.toList().sortedByDescending { it.second }
        if (grouped.isEmpty()) body.addView(card { addView(label("Ainda não há despesas neste mês.", 14, muted, false)) })
        grouped.forEach { (category, amount) ->
            body.addView(card {
                addView(label(category, 14, ink, true)); addView(label(currency.format(amount), 16, pink, true).apply { setPadding(0, 4, 0, 0) })
            })
        }
        if (!premium()) body.addView(label("No Premium, você tem acesso ao histórico financeiro completo.", 13, muted, false).apply { setPadding(6, 12, 6, 0) })
    }

    private fun plans(body: LinearLayout) {
        body.addView(label("Escolha seu plano", 22, ink, true).apply { setPadding(4, 4, 0, 12) })
        body.addView(card {
            addView(label("GRATUITO", 12, muted, true)); addView(label("R$ 0,00", 25, ink, true).apply { setPadding(0, 6, 0, 8) })
            addView(label("• Até 10 lançamentos por mês\n• Controle financeiro\n• Resumo básico", 14, ink, false))
            addView(label(if (premium()) "Plano anterior" else "Seu plano atual", 13, muted, true).apply { setPadding(0, 12, 0, 0) })
        })
        body.addView(card {
            addView(label("PREMIUM", 12, pink, true)); addView(label("R$ 9,90/mês", 25, ink, true).apply { setPadding(0, 6, 0, 8) })
            addView(label("• Lançamentos ilimitados\n• Relatórios completos\n• Histórico financeiro", 14, ink, false))
            if (premium()) addView(label("Você já é Premium ✨", 14, Color.rgb(65, 142, 111), true).apply { setPadding(0, 12, 0, 0) })
            else addView(button("Assinar Premium", pink) {
                AlertDialog.Builder(this).setTitle("Assinar Premium")
                    .setMessage("Esta é uma simulação acadêmica. Nenhum pagamento será realizado. Deseja ativar o Premium?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Ativar Premium") { _, _ -> prefs.edit().putBoolean("premium", true).apply(); render(); Toast.makeText(this, "Premium ativado!", Toast.LENGTH_SHORT).show() }.show()
            }.apply { setPadding(0, 12, 0, 0) })
        })
        body.addView(label("A assinatura é simulada e não envolve cobrança real.", 12, muted, false).apply { setPadding(6, 4, 6, 0) })
    }

    private fun entryRow(entry: FinanceEntry, canDelete: Boolean): View = card {
        orientation = LinearLayout.HORIZONTAL
        val details = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        details.addView(label(entry.description, 15, ink, true))
        details.addView(label("${entry.category} • ${entry.date}", 12, muted, false).apply { setPadding(0, 4, 0, 0) })
        addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        addView(label((if (entry.type == "RECEITA") "+ " else "− ") + currency.format(entry.amount), 14, if (entry.type == "RECEITA") Color.rgb(65, 142, 111) else pink, true).apply { gravity = Gravity.CENTER_VERTICAL })
        if (canDelete) addView(TextView(this@MainActivity).apply {
            text = "  ✕"; textSize = 16f; setTextColor(pink); gravity = Gravity.CENTER; setPadding(8, 8, 2, 8)
            setOnClickListener { AlertDialog.Builder(this@MainActivity).setMessage("Excluir esta despesa?").setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ -> db.delete(entry.id); render() }.show() }
        })
    }

    private fun showEntryDialog(type: String) {
        if (!premium() && entriesThisMonth().size >= 10) {
            AlertDialog.Builder(this).setTitle("Limite do plano gratuito")
                .setMessage("Você já utilizou os 10 lançamentos deste mês. Ative o Premium para continuar.")
                .setPositiveButton("Ver planos") { _, _ -> page = "Planos"; render() }.setNegativeButton("Fechar", null).show()
            return
        }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0) }
        val description = EditText(this).apply { hint = "Descrição"; singleLine = true }
        val amount = EditText(this).apply { hint = "Valor (ex.: 125,50)"; inputType = 8194; singleLine = true }
        val date = EditText(this).apply { hint = "Data (AAAA-MM-DD)"; setText(today()); singleLine = true }
        val spinner = Spinner(this)
        val cats = if (type == "RECEITA") arrayOf("Salário", "Freelance", "Investimentos", "Outros") else categories.filter { it !in listOf("Salário", "Freelance") }.toTypedArray()
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cats)
        layout.addView(description); layout.addView(amount); layout.addView(date); layout.addView(spinner)
        AlertDialog.Builder(this).setTitle(if (type == "RECEITA") "Nova receita" else "Nova despesa").setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val desc = description.text.toString().trim()
                        val value = amount.text.toString().trim().replace(".", "").replace(",", ".").toDoubleOrNull()
                        val dateValue = date.text.toString().trim()
                        if (desc.isEmpty() || value == null || value <= 0 || !dateValue.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                            Toast.makeText(this, "Preencha descrição, valor positivo e data AAAA-MM-DD.", Toast.LENGTH_LONG).show(); return@setOnClickListener
                        }
                        db.add(type, desc, value, dateValue, spinner.selectedItem.toString())
                        dialog.dismiss(); render()
                    }
                }
                dialog.show()
            }
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun card(content: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16); background = rounded(Color.WHITE, 18)
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = 10; layoutParams = lp
        content()
    }

    private fun button(text: String, color: Int, action: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        setPadding(18, 14, 18, 14); background = rounded(color, 24); setOnClickListener { action() }
        val lp = LinearLayout.LayoutParams(-1, -2); lp.topMargin = 8; lp.bottomMargin = 4; layoutParams = lp
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }
}
