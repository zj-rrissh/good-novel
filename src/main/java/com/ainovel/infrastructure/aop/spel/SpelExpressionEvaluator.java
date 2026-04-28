package com.ainovel.infrastructure.aop.spel;

import com.ainovel.security.auth.context.CurrentUserHolder;
import java.lang.reflect.Method;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Component
public class SpelExpressionEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public String evaluate(String expression, Method method, Object[] args) {
        if (expression == null || expression.isBlank()) {
            return "";
        }
        EvaluationContext context = buildContext(method, args);
        Object value = parser.parseExpression(expression).getValue(context);
        return value == null ? "" : value.toString();
    }

    private EvaluationContext buildContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        CurrentUserHolder.get().ifPresent(user ->
                context.setVariable("currentUserId", user.userId()));
        return context;
    }
}
