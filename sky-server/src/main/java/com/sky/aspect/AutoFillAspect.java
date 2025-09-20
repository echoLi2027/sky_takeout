package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlbeans.InterfaceExtension;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Component
@Aspect
@Slf4j
public class AutoFillAspect {

//    point cut
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

//    autofill should assign value before insert/update obj in db, which means should use @before anno
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
//        debug, make sure before dig into mapper methods, this method can execute first
        log.info("*******aop alert: start auto fill public attrs.....");

//        1. get initialized obj: args of intercepted method
//        1.1 get method signature obj
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
//        1.2 get anno obj on the method
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
//        1.3 get db operation type
        OperationType operationType = autoFill.value();

//        2. prepare data that need to autofilled
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0){
            return;
        }
//        actually this is the emp/category obj that we want to insert into db
        Object entity = args[0];

//        2.1 prepare assigned data
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();


//        3. according to different operation type assign values via reflection
        if (operationType == OperationType.INSERT){
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

//                set the pojo's attr, and in order to set this attr, we need to find the setter method obj first
//                invoke that setter, and tie the assigned value with respective pojo
                setCreateTime.invoke(entity,now);
                setCreateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (operationType == OperationType.UPDATE) {
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


}
