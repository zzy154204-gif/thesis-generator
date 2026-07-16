import type { FormItemRule } from 'element-plus'

/** 学号校验：8-12 位数字 */
export const studentIdRule: FormItemRule = {
  pattern: /^\d{8,12}$/,
  message: '学号为8-12位数字',
  trigger: 'blur',
}

/** 密码校验：≥6 位，含字母和数字 */
export const passwordRule: FormItemRule = {
  pattern: /^(?=.*[a-zA-Z])(?=.*\d)/,
  message: '密码需包含字母和数字',
  trigger: 'blur',
}

/** 邮箱格式校验 */
export const emailRule: FormItemRule = {
  type: 'email',
  message: '邮箱格式不正确',
  trigger: 'blur',
}

/** 必填校验 */
export function requiredRule(label: string): FormItemRule {
  return { required: true, message: `请输入${label}`, trigger: 'blur' }
}

/** 长度校验 */
export function lengthRule(min: number, max: number): FormItemRule {
  return { min, max, message: `长度为${min}-${max}个字符`, trigger: 'blur' }
}

/** 确认密码校验工厂 */
export function confirmPasswordRule(passwordGetter: () => string): FormItemRule {
  return {
    required: true,
    message: '请确认密码',
    trigger: 'blur',
    validator: (_rule, value, callback) => {
      if (value !== passwordGetter()) {
        callback(new Error('两次密码不一致'))
      } else {
        callback()
      }
    },
  } as FormItemRule
}

/** 学号校验规则数组 */
export const studentIdRules: FormItemRule[] = [
  { required: true, message: '请输入学号', trigger: 'blur' },
  { pattern: /^\d{8,12}$/, message: '学号为8-12位数字', trigger: 'blur' },
]

/** 密码校验规则数组 */
export const passwordRules: FormItemRule[] = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 6, message: '密码长度不少于6位', trigger: 'blur' },
  { pattern: /^(?=.*[a-zA-Z])(?=.*\d)/, message: '密码需包含字母和数字', trigger: 'blur' },
]

/** 姓名校验规则数组 */
export const nameRules: FormItemRule[] = [
  { required: true, message: '请输入姓名', trigger: 'blur' },
  { min: 2, max: 20, message: '姓名为2-20个字符', trigger: 'blur' },
]
