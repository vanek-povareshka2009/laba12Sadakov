package org.example;

import jakarta.persistence.*;
import org.hibernate.annotations.OptimisticLock;



@Entity
@Table(name = "items")
public class Items {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "val")
    private long val;

    @OptimisticLock(excluded = true)
    @Column(name = "val1")
    private long val1;

    @Version
    @Column(name = "version")
    private long version;

    public Items() {
        this.val = 0;
        this.val1 = 0;
    }

    public long getId() {
        return id;
    }

    public long getVal() {
        return val;
    }

    public long getVal1() {
        return val1;
    }

    public long getVersion() {
        return version;
    }

    public void setVal(long val) {
        this.val = val;
    }

    public void setVal1(long val1) {
        this.val1 = val1;
    }
}
