package org.example;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import jakarta.persistence.*;;
import java.util.concurrent.CountDownLatch;
import java.util.Scanner;

public class Main {

    public static final int THREAD_COUNT = 8;
    public static final int THREAD_COUNT1 = 8;
    static SessionFactory configuration = new Configuration()
            .addAnnotatedClass(Items.class)
            .configure("hibernate.cfg.xml").buildSessionFactory();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Выберите тип блокировки: 1 - оптимистичная, 2 - пессимистичная");
        int lockType = scanner.nextInt();

        long startTime = System.currentTimeMillis();

        try {
            if (lockType == 1) {
                runOptimisticLock();
            } else if (lockType == 2) {
                runPessimisticLock();
            } else {
                System.out.println("Некорректный выбор типа блокировки.");
            }
        } finally {
            // Close the session factory only after all operations are done
            configuration.close();
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        //System.out.println("Затраченное время " + elapsedTime + " миллисекунд");
        //System.out.println("Количество OptimisticLockException: " + cntOptimisticLock);
    }

    static int cntOptimisticLock = 0;

    public static void runOptimisticLock() {
        long time = System.currentTimeMillis();
        Session session = configuration.openSession();
        try {
            session.beginTransaction();
            for (int i = 0; i < 40; i++) {
                Items item = new Items();
                session.persist(item);
            }
            session.getTransaction().commit();
            threadTest(true);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }


    public static void runPessimisticLock() {
        long time = System.currentTimeMillis();
        Session session = configuration.openSession();
        try {
            for (int i = 0; i < 40; i++) {
                session.beginTransaction();
                Items item = new Items();
                session.persist(item);
                session.getTransaction().commit();
            }
            threadTest(false);
            threadTestval1();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public static void threadTest(boolean isOptimisticLock) {
        CountDownLatch countDownLatch = new CountDownLatch(THREAD_COUNT);
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int ii = i + 1;
            threads[i] = new Thread(() -> {
                for (int k = 0; k < 20000; k++) {
                    boolean upd = false;
                    while (!upd) {
                        Session session = configuration.getCurrentSession();
                        Long rndRow = (long) ((Math.random() * 40) + 1);
                        try {
                            session.beginTransaction();

                            String hqlQuery = "SELECT items FROM Items items WHERE items.id = :id";
                            Query<Items> query = session.createQuery(hqlQuery, Items.class)
                                    .setParameter("id", rndRow);

                            if (isOptimisticLock) {
                                // Optimistic Locking: Increment the value without locking
                                Items items = query.uniqueResult();
                                items.setVal(items.getVal() + 1);

                            } else {
                                // Pessimistic Locking: Obtain a pessimistic write lock
                                Items items = query.setLockMode(LockModeType.PESSIMISTIC_WRITE).uniqueResult();
                                items.setVal(items.getVal() + 1);
                            }

                            Thread.sleep(5);
                            session.getTransaction().commit();
                            upd = true;
                        } catch (OptimisticLockException e) {
                            cntOptimisticLock++;
                            session.getTransaction().rollback();
                        } catch (HibernateException | InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            session.close();
                        }
                    }
                }
                countDownLatch.countDown();
            });
            threads[i].start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Session session = configuration.openSession();
            session.beginTransaction();
            String hqlQuery = "SELECT SUM(val) FROM Items";
            Query<Long> query = session.createQuery(hqlQuery, Long.class);
            Long sum = query.uniqueResult();
            System.out.println("Сумма элементов: " + sum);
            //System.out.println("Сумма элементов: val1:" + sum);
            session.getTransaction().commit();
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void threadTestval1() {
        CountDownLatch countDownLatch = new CountDownLatch(THREAD_COUNT1);
        Thread[] threads = new Thread[THREAD_COUNT1];
        for (int i = 0; i < THREAD_COUNT1; i++) {
            final int ii = i + 1;
            threads[i] = new Thread(() -> {
                for (int k = 0; k < 20000; k++) {
                    boolean upd = false;
                    while (!upd) {
                        Session session = configuration.getCurrentSession();
                        Long rndRow = (long) ((Math.random() * 40) + 1);
                        try {
                            session.beginTransaction();

                            String hqlQuery = "SELECT items FROM Items items WHERE items.id = :id";
                            Query<Items> query = session.createQuery(hqlQuery, Items.class)
                                    .setParameter("id", rndRow);


                            Items items = query.setLockMode(LockModeType.PESSIMISTIC_WRITE).uniqueResult();
                            items.setVal1(items.getVal1() + 1);

                            Thread.sleep(5);
                            session.getTransaction().commit();
                            upd = true;
                        } catch (OptimisticLockException e) {
                            cntOptimisticLock++;
                            session.getTransaction().rollback();
                        } catch (HibernateException | InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            session.close();
                        }
                    }
                }
                countDownLatch.countDown();
            });
            threads[i].start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Session session = configuration.openSession();
            session.beginTransaction();

            String hqlQuery = "SELECT SUM(val1) FROM Items";
            Query<Long> query = session.createQuery(hqlQuery, Long.class);
            Long sum1 = query.uniqueResult();
            System.out.println("Сумма элементов val1: " + sum1);

            Query<Items> updateQuery = session.createQuery("UPDATE Items SET val1 = val1");
            updateQuery.executeUpdate();


            session.getTransaction().commit();
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
