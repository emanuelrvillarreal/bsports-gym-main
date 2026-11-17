package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.ReservaAmenityCobroTipoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Table(name = "mercado_pago_cobros")
@Entity
public class MercadoPagoCobro extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", nullable = false)
    private User registradoPor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "tipo", nullable = false, length = 20)
    private MercadoPagoCobroTipoEnum tipo;

    @Column(name = "titulo")
    private String titulo;

    @Column(name = "detalle")
    private String detalle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercado_pago_cobro_intent_id", unique = true)
    private MercadoPagoCobroIntent mercadoPagoCobroIntent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_acceso_id")
    private EventoAcceso eventoAcceso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "reserva_cobro_tipo", length = 10)
    private ReservaAmenityCobroTipoEnum reservaCobroTipo;

    @OneToMany(mappedBy = "mercadoPagoCobro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MercadoPagoCobroItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "mercadoPagoCobro", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private EventoAccesoMercadoPagoCobroDatos eventoAccesoDatos;

    public void asignarEventoAccesoDatos(EventoAccesoHistorialTipoEnum tipo, Member member, String nombre) {
        EventoAccesoMercadoPagoCobroDatos datos = new EventoAccesoMercadoPagoCobroDatos();
        datos.setMercadoPagoCobro(this);
        datos.setTipo(tipo);
        datos.setMember(member);
        datos.setNombre(nombre);
        this.eventoAccesoDatos = datos;
    }

    public void addItem(Cuota cuota) {
        addItem(cuota, cuota.montoFinal(), cuota.getConcepto());
    }

    public void addItem(Cuota cuotaRef, BigDecimal monto, String detalle) {
        MercadoPagoCobroItem item = new MercadoPagoCobroItem();
        item.setMercadoPagoCobro(this);
        item.setCuota(cuotaRef);
        item.setMonto(monto);
        item.setDetalle(detalle);
        items.add(item);
    }

    public void addItemSinCuota(BigDecimal monto, String titulo, String detalle) {
        MercadoPagoCobroItem item = new MercadoPagoCobroItem();
        item.setMercadoPagoCobro(this);
        item.setMonto(monto);
        item.setTitulo(titulo);
        item.setDetalle(detalle);
        items.add(item);
    }
}
