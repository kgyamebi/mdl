import { motion, useReducedMotion } from 'framer-motion';

type AnimatedStatProps = {
  value: string;
  label: string;
  delay?: number;
};

export function AnimatedStat({ value, label, delay = 0 }: AnimatedStatProps) {
  const reduce = useReducedMotion();

  return (
    <motion.div
      className="strip__stat"
      initial={reduce ? false : { opacity: 0, y: 20, scale: 0.94 }}
      whileInView={reduce ? undefined : { opacity: 1, y: 0, scale: 1 }}
      viewport={{ once: true, amount: 0.5 }}
      transition={{ duration: 0.7, delay, ease: [0.22, 1, 0.36, 1] }}
    >
      <motion.strong
        initial={reduce ? false : { opacity: 0.35 }}
        whileInView={reduce ? undefined : { opacity: 1 }}
        viewport={{ once: true }}
        transition={{ duration: 1.1, delay: delay + 0.1 }}
      >
        {value}
      </motion.strong>
      <span>{label}</span>
    </motion.div>
  );
}
