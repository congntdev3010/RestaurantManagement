package feature.reservation.controller;

import feature.reservation.dao.ReservationDAO;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Mỗi phút tự huỷ các đơn đã duyệt mà khách đến muộn quá 30 phút so với giờ đặt. */
@WebListener
public class LateArrivalCleaner implements ServletContextListener {

    public static final int GRACE_MINUTES = 30;
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "late-arrival-cleaner");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            try (ReservationDAO dao = new ReservationDAO()) {
                int n = dao.cancelLateArrivals(GRACE_MINUTES);
                if (n > 0) event.getServletContext().log("Tự huỷ " + n + " đơn đặt bàn khách đến muộn.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) scheduler.shutdownNow();
    }
}
