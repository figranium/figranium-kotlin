package dev.figranium.sdk

import java.util.UUID

/** Creates Figranium's required `{$name}` variable token. */
fun variable(name: String): String {
    require(name.isNotBlank()) { "Variable name must not be blank" }
    return "{\$$name}"
}

/** Factory helpers for every Figranium browser action. */
object Actions {
    fun action(value: Action): Action = if (value.id == null) value.copy(id = "act_${value.type}_${UUID.randomUUID()}") else value
    fun navigate(url: String) = action(Action("navigate", value = url))
    fun click(selector: String, kind: String = "single") = action(Action("click", selector = selector, clickType = kind.takeUnless { it == "single" }))
    fun check(selector: String) = action(Action("check", selector = selector))
    fun uncheck(selector: String) = action(Action("uncheck", selector = selector))
    fun dragAndDrop(selector: String, to: String) = action(Action("drag_and_drop", selector = selector, targetSelector = to))
    fun reload() = action(Action("reload"))
    fun select(selector: String, value: String) = action(Action("select", selector = selector, value = value))
    fun type(selector: String, value: String, mode: String = "replace") = action(Action("type", selector = selector, value = value, typeMode = mode))
    fun wait(seconds: Double) = action(Action("wait", value = seconds.toString()))
    fun waitFor(selector: String) = action(Action("wait_selector", selector = selector))
    fun waitForDownloads(timeout: Double? = null) = action(Action("wait_downloads", value = timeout?.toString()))
    fun press(key: String, selector: String? = null) = action(Action("press", selector = selector, key = key))
    fun scroll(selector: String? = null, value: String? = null) = action(Action("scroll", selector = selector, value = value))
    fun javascript(script: String, varName: String? = null) = action(Action("javascript", value = script, varName = varName))
    fun csv(value: String? = null, selector: String? = null, varName: String? = null) = action(Action("csv", selector = selector, value = value, varName = varName))
    fun hover(selector: String) = action(Action("hover", selector = selector))
    fun merge(name: String, value: String) = action(Action("merge", value = value, varName = name))
    fun screenshot(name: String? = null) = action(Action("screenshot", value = name))
    fun conditional(type: String, selector: String? = null, value: String? = null, variable: String? = null, variableType: String? = null, operation: String? = null, comparisonValue: String? = null) = action(Action(type, selector = selector, value = value, conditionVar = variable, conditionVarType = variableType, conditionOp = operation, conditionValue = comparisonValue))
    fun ifAction(selector: String? = null, value: String? = null, variable: String? = null, variableType: String? = null, operation: String? = null, comparisonValue: String? = null) = conditional("if", selector, value, variable, variableType, operation, comparisonValue)
    fun whileAction(selector: String? = null, value: String? = null, variable: String? = null, variableType: String? = null, operation: String? = null, comparisonValue: String? = null) = conditional("while", selector, value, variable, variableType, operation, comparisonValue)
    fun elseBlock() = action(Action("else")); fun end() = action(Action("end")); fun repeatCount(count: Int) = action(Action("repeat", value = count.toString()))
    fun forEach(selector: String? = null, value: String? = null, varName: String? = null) = action(Action("foreach", selector = selector, value = value, varName = varName))
    fun stop(outcome: String = "success") = action(Action("stop", value = outcome)); fun set(name: String, value: String) = action(Action("set", value = value, varName = name))
    fun onError(value: String? = null) = action(Action("on_error", value = value)); fun doNothing() = action(Action("do_nothing")); fun start(taskId: String) = action(Action("start", value = taskId))
    fun request(url: String, method: String? = null, headers: String? = null, body: String? = null, varName: String? = null) = action(Action("http_request", value = url, method = method, headers = headers, body = body, varName = varName))
    fun getContent(selector: String? = null, varName: String? = null) = action(Action("get_content", selector = selector, varName = varName))
    fun captcha(type: String, captchaType: String? = null, selector: String? = null, varName: String? = null, timeout: Double? = null) = action(Action(type, selector = selector, varName = varName, captchaType = captchaType, timeout = timeout))
    fun solveCaptcha(captchaType: String? = null, selector: String? = null, varName: String? = null, timeout: Double? = null) = captcha("solve_captcha", captchaType, selector, varName, timeout)
    fun waitForCaptcha(captchaType: String? = null, selector: String? = null, varName: String? = null, timeout: Double? = null) = captcha("wait_captcha", captchaType, selector, varName, timeout)
    fun upload(selector: String? = null, cabinetId: String? = null, markAsUploaded: Boolean? = null) = action(Action("upload", selector = selector, cabinetId = cabinetId, markAsUploaded = markAsUploaded))
    fun finalizeUploads() = action(Action("finalize_uploads"))
}
